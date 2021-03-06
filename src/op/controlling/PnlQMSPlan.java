package op.controlling;

import com.jidesoft.pane.CollapsiblePane;
import com.jidesoft.pane.CollapsiblePanes;
import com.jidesoft.pane.event.CollapsiblePaneAdapter;
import com.jidesoft.pane.event.CollapsiblePaneEvent;
import com.jidesoft.popup.JidePopup;
import com.jidesoft.swing.JideBoxLayout;
import entity.qms.*;
import entity.system.Commontags;
import gui.GUITools;
import gui.interfaces.CleanablePanel;
import gui.interfaces.DefaultCPTitle;
import op.OPDE;
import op.care.sysfiles.DlgFiles;
import op.system.InternalClassACL;
import op.threads.DisplayManager;
import op.threads.DisplayMessage;
import op.tools.DlgYesNo;
import op.tools.SYSConst;
import op.tools.SYSTools;
import org.apache.commons.collections.Closure;
import org.jdesktop.swingx.VerticalLayout;
import org.joda.time.LocalDate;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * ACL
 * =============
 * see PnlControlling
 */
public class PnlQMSPlan extends CleanablePanel {

    CollapsiblePanes cpsMain;

    private HashMap<Qms, CollapsiblePane> mapQms2Panel;
    private HashMap<String, CollapsiblePane> cpMap;
    private ArrayList<Qmsplan> listQMSPlans;
    //    private ArrayList<Qmsplan> listQMSPlans;
    private final Qmsplan expandMe;

    //    private final int MAX_TEXT_LENGTH = 65;
    private final int MAX_MONTHS_IN_ADVANCE_TO_CONFIRM_QMS = 6;
    private Commontags commontag;
    private boolean closedOnes2;

    public PnlQMSPlan(Qmsplan expandMe) {
        super("opde.controlling.qms.pnlqmsplan");
        listQMSPlans = new ArrayList<>();
        this.expandMe = expandMe;
        cpMap = new HashMap<>();
        mapQms2Panel = new HashMap<>();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        cpsMain = new CollapsiblePanes();
        add(new JScrollPane(cpsMain));
        reload();
    }

//    public ArrayList<Qmsplan> getListQMSPlans() {
//        return listQMSPlans;
//    }

    @Override
    public void cleanup() {
        super.cleanup();
        cpMap.clear();
        mapQms2Panel.clear();
        cpsMain.removeAll();
        listQMSPlans.clear();
    }


    public void reload(Commontags commontag, boolean closedOnes2) {
        this.commontag = commontag;
        this.closedOnes2 = closedOnes2;
        reload();
    }

    private void reloadData() {
        listQMSPlans.clear();
        if (commontag != null) {
            listQMSPlans.addAll(QmsplanTools.getAll(commontag));
        } else if (closedOnes2) {
            listQMSPlans.addAll(QmsplanTools.getAll());
        } else {
            listQMSPlans.addAll(QmsplanTools.getAllActive());
        }
    }

    @Override
    public void reload() {

        helpkey = OPDE.getAppInfo().getInternalClasses().containsKey(internalClassID) ? OPDE.getAppInfo().getInternalClasses().get(internalClassID).getHelpurl() : null;
        OPDE.getDisplayManager().setMainMessage(internalClassID);

        cpMap.clear();
        mapQms2Panel.clear();
        reloadData();
        for (Qmsplan qmsplan : listQMSPlans) {
            createCP4(qmsplan);
        }
        buildPanel();
    }

    @Override
    public String getInternalClassID() {
        return internalClassID;
    }

    private void buildPanel() {
        cpsMain.removeAll();
        cpsMain.setLayout(new JideBoxLayout(cpsMain, JideBoxLayout.Y_AXIS));

        for (Qmsplan qmsplan : listQMSPlans) {
            cpsMain.add(cpMap.get(qmsplan.getId() + ".qmsplan"));
        }
        cpsMain.addExpansion();

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }


    private CollapsiblePane createCP4(final Qmsplan qmsplan) {
        final String key = qmsplan.getId() + ".qmsplan";
        if (!cpMap.containsKey(key)) {
            cpMap.put(key, new CollapsiblePane());
            try {
                cpMap.get(key).setCollapsed(expandMe == null || !qmsplan.equals(expandMe));
            } catch (PropertyVetoException e) {
                // Bah!
            }
        }
        final CollapsiblePane cpPlan = cpMap.get(key);

        String title = qmsplan.getTitle();

        DefaultCPTitle cptitle = new DefaultCPTitle(title, e -> {
            try {
                cpPlan.setCollapsed(!cpPlan.isCollapsed());
            } catch (PropertyVetoException pve) {
                // BAH!
            }
        });

        cptitle.getButton().setFont(SYSConst.ARIAL24BOLD);
        cptitle.getButton().setForeground(Color.BLACK);
        cpPlan.setBackground(GUITools.blend(qmsplan.getColor(), Color.WHITE, 0.3f));

        cpPlan.setTitleLabelComponent(cptitle.getMain());
        cpPlan.setSlidingDirection(SwingConstants.SOUTH);
//            cpPlan.setBackground(getColor(cat)[SYSConst.medium2]);
        cpPlan.setOpaque(true);
        cpPlan.setHorizontalAlignment(SwingConstants.LEADING);


        /***
         *      __  __
         *     |  \/  | ___ _ __  _   _
         *     | |\/| |/ _ \ '_ \| | | |
         *     | |  | |  __/ | | | |_| |
         *     |_|  |_|\___|_| |_|\__,_|
         *
         */
        final JButton btnMenu = new JButton(SYSConst.icon22menu);
        btnMenu.setPressedIcon(SYSConst.icon22Pressed);
        btnMenu.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btnMenu.setAlignmentY(Component.CENTER_ALIGNMENT);
        btnMenu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMenu.setContentAreaFilled(false);
        btnMenu.setBorder(null);
        btnMenu.addActionListener(e -> {
            JidePopup popup = new JidePopup();
            popup.setMovable(false);
            popup.getContentPane().setLayout(new BoxLayout(popup.getContentPane(), BoxLayout.LINE_AXIS));
            popup.setOwner(btnMenu);
            popup.removeExcludedComponent(btnMenu);
            JPanel pnl = getMenu(qmsplan);
            popup.getContentPane().add(pnl);
            popup.setDefaultFocusComponent(pnl);

            GUITools.showPopup(popup, SwingConstants.WEST);
        });
        btnMenu.setEnabled(qmsplan.isActive());
        cptitle.getRight().add(btnMenu);


        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.UPDATE, "opde.controlling")) {
            /***
             *       ___           _____   __  __   ____       _              _       _
             *      / _ \ _ __    / / _ \ / _|/ _| / ___|  ___| |__   ___  __| |_   _| | ___
             *     | | | | '_ \  / / | | | |_| |_  \___ \ / __| '_ \ / _ \/ _` | | | | |/ _ \
             *     | |_| | | | |/ /| |_| |  _|  _|  ___) | (__| | | |  __/ (_| | |_| | |  __/
             *      \___/|_| |_/_/  \___/|_| |_|   |____/ \___|_| |_|\___|\__,_|\__,_|_|\___|
             *
             */
            final JToggleButton btnOnOff = GUITools.getNiceToggleButton(null);
            btnOnOff.setSelected(qmsplan.isActive());
            btnOnOff.addItemListener(e -> {
                EntityManager em = OPDE.createEM();
                try {
                    em.getTransaction().begin();
                    Qmsplan myQMSPlan = em.merge(qmsplan);
                    em.lock(myQMSPlan, LockModeType.OPTIMISTIC);
                    myQMSPlan.setState(e.getStateChange() == ItemEvent.SELECTED ? QmsplanTools.STATE_ACTIVE : QmsplanTools.STATE_INACTIVE);

                    em.getTransaction().commit();

//                        listQMSPlans.set(listQMSPlans.indexOf(qmsplan), myQMSPlan);

                    createCP4(myQMSPlan);
                    buildPanel();
                } catch (OptimisticLockException ole) {
                    OPDE.warn(ole);
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }

                    OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                    reload();
                } catch (Exception ex) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    OPDE.fatal(ex);
                } finally {
                    em.close();
                }
            });
            btnOnOff.setToolTipText(SYSTools.xx("opde.controlling.qms.plan.onoff.tooltip"));
            cptitle.getRight().add(btnOnOff);
        }

        cpPlan.addCollapsiblePaneListener(new CollapsiblePaneAdapter() {
            @Override
            public void paneExpanded(CollapsiblePaneEvent collapsiblePaneEvent) {
                cpPlan.setContentPane(createContent4(qmsplan));
            }
        });

        if (!cpPlan.isCollapsed()) {
            cpPlan.setContentPane(createContent4(qmsplan));

        }

        return cpPlan;
    }


    private CollapsiblePane createCP4(final Qmssched qmssched) {
        final String key = qmssched.getId() + ".qmssched";
        if (!cpMap.containsKey(key)) {
            cpMap.put(key, new CollapsiblePane());
            try {
                cpMap.get(key).setCollapsed(expandMe == null || !qmssched.getQmsplan().equals(expandMe));
            } catch (PropertyVetoException e) {
                // Bah!
            }
        }
        final CollapsiblePane cpSched = cpMap.get(key);

        String title = SYSTools.toHTMLForScreen(QmsschedTools.getAsHTML(qmssched));

        DefaultCPTitle cptitle = new DefaultCPTitle(title, e -> {
            try {
                cpSched.setCollapsed(!cpSched.isCollapsed());
            } catch (PropertyVetoException pve) {
                // BAH!
            }
        });

        cptitle.getAdditionalIconPanel().add(new JLabel(QmsschedTools.getIcon(qmssched)));


        cpSched.setBackground(GUITools.blend(qmssched.getQmsplan().getColor(), Color.WHITE, 0.1f));
        cpSched.setTitleLabelComponent(cptitle.getMain());
        cpSched.setSlidingDirection(SwingConstants.SOUTH);
        //            cpPlan.setBackground(getColor(cat)[SYSConst.medium2]);
        cpSched.setOpaque(true);
        cpSched.setHorizontalAlignment(SwingConstants.LEADING);

        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.UPDATE, "opde.controlling")) {
            /***
             *      _____    _ _ _     ____       _              _       _
             *     | ____|__| (_) |_  / ___|  ___| |__   ___  __| |_   _| | ___
             *     |  _| / _` | | __| \___ \ / __| '_ \ / _ \/ _` | | | | |/ _ \
             *     | |__| (_| | | |_   ___) | (__| | | |  __/ (_| | |_| | |  __/
             *     |_____\__,_|_|\__| |____/ \___|_| |_|\___|\__,_|\__,_|_|\___|
             *
             */
            final JButton btnEdit = GUITools.getTinyButton("misc.msg.edit", SYSConst.icon22edit3);
            btnEdit.addActionListener(e -> {

                if (!QmsschedTools.isUnused(qmssched)) {
                    OPDE.getDisplayManager().addSubMessage(new DisplayMessage("misc.msg.already.used.cant.edit"));
                    return;
                }

                final JidePopup popup = new JidePopup();
                PnlQMSSchedule pnlQMSSchedule = new PnlQMSSchedule(qmssched, o -> {
                    popup.hidePopup();
                    if (o != null) {
                        EntityManager em = OPDE.createEM();
                        try {
                            em.getTransaction().begin();
                            Qmsplan myQMSPlan = em.merge(qmssched.getQmsplan());
                            em.lock(myQMSPlan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                            Qmssched myQmssched = em.merge((Qmssched) o);
                            em.lock(myQmssched, LockModeType.OPTIMISTIC);
                            myQMSPlan.getQmsschedules().set(myQMSPlan.getQmsschedules().indexOf(qmssched), myQmssched);
                            em.getTransaction().commit();
//                                    listQMSPlans.set(listQMSPlans.indexOf(qmssched.getQmsplan()), myQMSPlan);
                            createCP4(myQMSPlan);
                            reloadData();
                            buildPanel();
                        } catch (OptimisticLockException ole) {
                            OPDE.warn(ole);
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }

                            OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            reload();
                        } catch (Exception e1) {
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            OPDE.fatal(e1);
                        } finally {
                            em.close();
                        }

                    }
                });
                popup.setMovable(false);
                popup.getContentPane().setLayout(new BoxLayout(popup.getContentPane(), BoxLayout.LINE_AXIS));

                popup.setOwner(btnEdit);
                popup.removeExcludedComponent(btnEdit);
                popup.getContentPane().add(pnlQMSSchedule);
                popup.setDefaultFocusComponent(pnlQMSSchedule);
                GUITools.showPopup(popup, SwingConstants.CENTER);
            });
            btnEdit.setEnabled(qmssched.isActive());
            cptitle.getRight().add(btnEdit);

        }

        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.DELETE, "opde.controlling") ||
                OPDE.getAppInfo().isAllowedTo(InternalClassACL.MANAGER, "opde.controlling")
                ) {
            /***
             *          _      _      _
             *       __| | ___| | ___| |_ ___
             *      / _` |/ _ \ |/ _ \ __/ _ \
             *     | (_| |  __/ |  __/ ||  __/
             *      \__,_|\___|_|\___|\__\___|
             *
             */

            final JButton btnDelete = GUITools.getTinyButton("misc.msg.delete", SYSConst.icon22delete);
            btnDelete.setPressedIcon(SYSConst.icon22deletePressed);
            btnDelete.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnDelete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDelete.setContentAreaFilled(false);
            btnDelete.setBorder(null);
            btnDelete.addActionListener(actionEvent -> {

                if (!QmsschedTools.isUnused(qmssched) && !OPDE.getAppInfo().isAllowedTo(InternalClassACL.MANAGER, "opde.controlling")) {
                    OPDE.getDisplayManager().addSubMessage(new DisplayMessage("misc.msg.already.used.cant.edit"));
                    return;
                }

                currentEditor = new DlgYesNo(SYSTools.xx("misc.questions.delete1") + "<br/><b>" + qmssched.getMeasure() + "</b><br/>" + SYSTools.xx("misc.questions.delete2"), SYSConst.icon48delete, o -> {
                    if (o.equals(JOptionPane.YES_OPTION)) {
                        EntityManager em = OPDE.createEM();
                        try {
                            em.getTransaction().begin();
                            Qmsplan myQMSPlan = em.merge(qmssched.getQmsplan());
                            em.lock(myQMSPlan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                            Qmssched myQmssched = em.merge(qmssched);
                            em.remove(myQmssched);
                            myQMSPlan.getQmsschedules().remove(myQmssched);
                            em.getTransaction().commit();

                            // Refresh Display
//                                    listQMSPlans.set(listQMSPlans.indexOf(qmssched.getQmsplan()), myQMSPlan);
                            createCP4(myQMSPlan);
                            reloadData();
                            buildPanel();

                        } catch (OptimisticLockException ole) {
                            OPDE.warn(ole);
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            reload();
                        } catch (Exception e) {
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            OPDE.fatal(e);
                        } finally {
                            em.close();
                            currentEditor = null;
                        }
                    }
                });
                currentEditor.setVisible(true);
            });
            btnDelete.setEnabled(qmssched.isActive());
            cptitle.getRight().add(btnDelete);

        }

        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.UPDATE, "opde.controlling")) {
            /***
             *       ___           _____   __  __   ____       _              _       _
             *      / _ \ _ __    / / _ \ / _|/ _| / ___|  ___| |__   ___  __| |_   _| | ___
             *     | | | | '_ \  / / | | | |_| |_  \___ \ / __| '_ \ / _ \/ _` | | | | |/ _ \
             *     | |_| | | | |/ /| |_| |  _|  _|  ___) | (__| | | |  __/ (_| | |_| | |  __/
             *      \___/|_| |_/_/  \___/|_| |_|   |____/ \___|_| |_|\___|\__,_|\__,_|_|\___|
             *
             */
            final JToggleButton btnOnOff = GUITools.getNiceToggleButton(null);
            btnOnOff.setSelected(qmssched.getState() == QmsschedTools.STATE_ACTIVE);
            btnOnOff.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnOnOff.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnOnOff.setContentAreaFilled(false);
            btnOnOff.setBorder(null);
            btnOnOff.addItemListener(e -> {
                EntityManager em = OPDE.createEM();
                try {
                    em.getTransaction().begin();
                    Qmsplan myQMSPlan = em.merge(qmssched.getQmsplan());
                    Qmssched myQmssched = em.merge(qmssched);
                    em.lock(myQMSPlan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                    em.lock(myQmssched, LockModeType.OPTIMISTIC);
                    myQmssched.setState(e.getStateChange() == ItemEvent.SELECTED ? QmsschedTools.STATE_ACTIVE : QmsschedTools.STATE_INACTIVE);

                    myQMSPlan.getQmsschedules().set(myQMSPlan.getQmsschedules().indexOf(qmssched), myQmssched);

                    em.getTransaction().commit();

//                        listQMSPlans.set(listQMSPlans.indexOf(qmssched.getQmsplan()), myQMSPlan);

                    createCP4(myQMSPlan);
                    reloadData();
                    buildPanel();
                } catch (OptimisticLockException ole) {
                    OPDE.warn(ole);
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }

                    OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                    reload();
                } catch (Exception ex) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    OPDE.fatal(ex);
                } finally {
                    em.close();
                }
            });
            btnOnOff.setEnabled(qmssched.getQmsplan().isActive());
            btnOnOff.setToolTipText(SYSTools.xx("opde.controlling.qms.schedule.onoff.tooltip"));
            cptitle.getRight().add(btnOnOff);
        }

        cpSched.addCollapsiblePaneListener(new CollapsiblePaneAdapter() {
            @Override
            public void paneExpanded(CollapsiblePaneEvent collapsiblePaneEvent) {
                cpSched.setContentPane(createContent4(qmssched));
            }
        });

        if (!cpSched.isCollapsed()) {
            cpSched.setContentPane(createContent4(qmssched));

        }

        return cpSched;
    }


    private JPanel createContent4(final Qmsplan qmsplan) {
        JPanel pnl = new JPanel(new VerticalLayout());

        pnl.setBackground(GUITools.blend(qmsplan.getColor(), Color.WHITE, 0.08f));
        pnl.setOpaque(true);

        String title = SYSTools.toHTMLForScreen(QmsplanTools.getAsHTML(qmsplan));
        pnl.add(new JLabel(title));

        final JButton btnNewSched = GUITools.createHyperlinkButton("opde.controlling.qms.pnlqmsplan.new.measure", SYSConst.icon22add, null);

        btnNewSched.addActionListener(e -> {
            final JidePopup popup = new JidePopup();
            PnlQMSSchedule pnlQMSSchedule = new PnlQMSSchedule(new Qmssched(qmsplan), o -> {
                popup.hidePopup();
                if (o != null) {
                    EntityManager em = OPDE.createEM();
                    try {
                        em.getTransaction().begin();
                        Qmsplan myQMSPlan = em.merge(qmsplan);
                        myQMSPlan.getQmsschedules().add(em.merge((Qmssched) o));
                        em.lock(myQMSPlan, LockModeType.OPTIMISTIC);
                        em.getTransaction().commit();
                        createCP4(myQMSPlan);
                        reloadData();
                        buildPanel();
                    } catch (OptimisticLockException ole) {
                        OPDE.warn(ole);
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }

                        OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                        reload();
                    } catch (Exception e1) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        OPDE.fatal(e1);
                    } finally {
                        em.close();
                    }

                }
            });

            popup.setMovable(false);
            popup.getContentPane().setLayout(new BoxLayout(popup.getContentPane(), BoxLayout.LINE_AXIS));

            popup.setOwner(btnNewSched);
            popup.removeExcludedComponent(btnNewSched);
            popup.getContentPane().add(pnlQMSSchedule);
            popup.setDefaultFocusComponent(pnlQMSSchedule);
            GUITools.showPopup(popup, SwingConstants.CENTER);
        });

        pnl.add(btnNewSched);
        for (Qmssched qmssched : qmsplan.getQmsschedules()) {
            pnl.add(createCP4(qmssched));
        }


        return pnl;
    }


    /**
     * this panel contains sub collapsible panes for every year with existing QMS (in the past) and 2 years in advance for possible future QMS.
     *
     * @param qmssched
     * @return
     */
    private JPanel createContent4(final Qmssched qmssched) {

        ArrayList<Qms> listQMS = new ArrayList<>(qmssched.getQmsList());
        Collections.sort(listQMS);

        JPanel pnlSched = new JPanel(new VerticalLayout());
        pnlSched.setOpaque(true);

        for (Qms qms : listQMS) {
            pnlSched.add(createCP4(qms));
        }

        return pnlSched;

    }

    private CollapsiblePane createCP4(final Qms qms) {

        if (mapQms2Panel.containsKey(qms)) {
            return mapQms2Panel.get(qms);
        }


        final CollapsiblePane cpQMS = new CollapsiblePane();

        cpQMS.setCollapseOnTitleClick(false);

        ActionListener applyActionListener = actionEvent -> {
            if (new LocalDate(qms.getTarget()).isAfter(new LocalDate().plusMonths(MAX_MONTHS_IN_ADVANCE_TO_CONFIRM_QMS))) {
                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("misc.msg.DateTooFarAway"));
                return;
            }
            if (!qms.isOpen()) {
                return;
            }
            if (!qms.getQmssched().isActive()) {
                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("misc.msg.inactive"));
                return;
            }

            EntityManager em = OPDE.createEM();
            try {
                em.getTransaction().begin();
                Qms myQms = em.merge(qms);

                Qmssched myQmssched = em.merge(myQms.getQmssched());
                Qmsplan myQmsplan = em.merge(myQms.getQmsplan());

                em.lock(myQmssched, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                em.lock(myQmsplan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

                myQms.setState(QmsTools.STATE_DONE);
                myQms.setUser(em.merge(OPDE.getLogin().getUser()));
                myQms.setActual(new java.util.Date());

                QmsTools.generate(myQmssched, 1);

                em.getTransaction().commit();

//                    listQMSPlans.set(listQMSPlans.indexOf(qms.getQmsplan()), myQmsplan);
                mapQms2Panel.remove(qms);
                createCP4(myQmsplan);
                reloadData();
                buildPanel();

            } catch (OptimisticLockException ole) {
                OPDE.warn(ole);
                OPDE.warn(ole);
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                if (ole.getMessage().indexOf("Class> entity.info.Resident") > -1) {
                    OPDE.getMainframe().emptyFrame();
                    OPDE.getMainframe().afterLogin();
                }
                OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
            } catch (RollbackException ole) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                if (ole.getMessage().indexOf("Class> entity.info.Resident") > -1) {
                    OPDE.getMainframe().emptyFrame();
                    OPDE.getMainframe().afterLogin();
                }
                OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                OPDE.fatal(e);
            } finally {
                em.close();
            }


        };


        String title = SYSTools.toHTMLForScreen(QmsTools.toHTML(qms));


        final DefaultCPTitle cptitle = new DefaultCPTitle(title, e -> {
            if (cpQMS.isCollapsible()) {
                try {
                    cpQMS.setCollapsed(cpQMS.isExpanded());
                } catch (PropertyVetoException e1) {
                    // bah!
                }
            }
        });


        cptitle.getButton().setIcon(QmsTools.getIcon(qms));


        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.USER1, "opde.controlling")) {
            if (qms.getQmsplan().isActive()) {

                /***
                 *      _     _            _                _
                 *     | |__ | |_ _ __    / \   _ __  _ __ | |_   _
                 *     | '_ \| __| '_ \  / _ \ | '_ \| '_ \| | | | |
                 *     | |_) | |_| | | |/ ___ \| |_) | |_) | | |_| |
                 *     |_.__/ \__|_| |_/_/   \_\ .__/| .__/|_|\__, |
                 *                             |_|   |_|      |___/
                 */
                JButton btnApply = new JButton(SYSConst.icon22apply);
                btnApply.setPressedIcon(SYSConst.icon22applyPressed);
                btnApply.setAlignmentX(Component.RIGHT_ALIGNMENT);
                btnApply.setToolTipText(SYSTools.xx("nursingrecords.bhp.btnApply.tooltip"));
                btnApply.addActionListener(applyActionListener);
                btnApply.setContentAreaFilled(false);
                btnApply.setBorder(null);
                btnApply.setEnabled(qms.getQmssched().isActive() && qms.isOpen());
                cptitle.getRight().add(btnApply);


                /***
                 *      _     _         ____       __
                 *     | |__ | |_ _ __ |  _ \ ___ / _|_   _ ___  ___
                 *     | '_ \| __| '_ \| |_) / _ \ |_| | | / __|/ _ \
                 *     | |_) | |_| | | |  _ <  __/  _| |_| \__ \  __/
                 *     |_.__/ \__|_| |_|_| \_\___|_|  \__,_|___/\___|
                 *
                 */
                final JButton btnRefuse = new JButton(SYSConst.icon22cancel);
                btnRefuse.setPressedIcon(SYSConst.icon22cancelPressed);
                btnRefuse.setAlignmentX(Component.RIGHT_ALIGNMENT);
                btnRefuse.setContentAreaFilled(false);
                btnRefuse.setBorder(null);
                btnRefuse.setToolTipText(SYSTools.toHTMLForScreen(SYSTools.xx("nursingrecords.bhp.btnRefuse.tooltip")));
                btnRefuse.addActionListener(actionEvent -> {
                    if (new LocalDate(qms.getTarget()).isAfter(new LocalDate().plusMonths(MAX_MONTHS_IN_ADVANCE_TO_CONFIRM_QMS))) {
                        OPDE.getDisplayManager().addSubMessage(new DisplayMessage("misc.msg.DateTooFarAway"));
                        return;
                    }
                    if (!qms.isOpen()) {
                        return;
                    }
//                        if (!qms.getQmssched().isActive()) {
//                            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("misc.msg.inactive"));
//                            return;
//                        }

                    EntityManager em = OPDE.createEM();
                    try {
                        em.getTransaction().begin();
                        Qms myQms = em.merge(qms);
                        Qmssched myQmssched = em.merge(myQms.getQmssched());
                        Qmsplan myQmsplan = em.merge(myQms.getQmsplan());

                        em.lock(myQmssched, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                        em.lock(myQmsplan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

                        myQms.setState(QmsTools.STATE_REFUSED);
                        myQms.setUser(em.merge(OPDE.getLogin().getUser()));
                        myQms.setActual(new java.util.Date());

                        em.getTransaction().commit();

//                            listQMSPlans.set(listQMSPlans.indexOf(qms.getQmsplan()), myQmsplan);
                        mapQms2Panel.remove(qms);
                        createCP4(myQmsplan);
                        reloadData();
                        buildPanel();
                    } catch (OptimisticLockException ole) {
                        OPDE.warn(ole);
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }

                        OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                        reload();
                    } catch (Exception e) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        OPDE.fatal(e);
                    } finally {
                        em.close();
                    }


                });
                btnRefuse.setEnabled(qms.getQmssched().isActive());
                cptitle.getRight().add(btnRefuse);


                /***
                 *      _     _         _____                 _
                 *     | |__ | |_ _ __ | ____|_ __ ___  _ __ | |_ _   _
                 *     | '_ \| __| '_ \|  _| | '_ ` _ \| '_ \| __| | | |
                 *     | |_) | |_| | | | |___| | | | | | |_) | |_| |_| |
                 *     |_.__/ \__|_| |_|_____|_| |_| |_| .__/ \__|\__, |
                 *                                     |_|        |___/
                 */
                final JButton btnEmpty = new JButton(SYSConst.icon22empty);
                btnEmpty.setPressedIcon(SYSConst.icon22emptyPressed);
                btnEmpty.setAlignmentX(Component.RIGHT_ALIGNMENT);
                btnEmpty.setContentAreaFilled(false);
                btnEmpty.setBorder(null);
                btnEmpty.setToolTipText(SYSTools.xx("nursingrecords.bhp.btnEmpty.tooltip"));
                btnEmpty.addActionListener(actionEvent -> {
                    if (qms.isOpen()) {
                        return;
                    }
//                        if (!qms.getQmssched().isActive()) {
//                            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("misc.msg.inactive"));
//                            return;
//                        }

                    EntityManager em = OPDE.createEM();
                    try {

                        em.getTransaction().begin();
                        Qms myQms = em.merge(qms);
                        Qmssched myQmssched = em.merge(myQms.getQmssched());
                        Qmsplan myQmsplan = em.merge(myQms.getQmsplan());

                        em.lock(myQmssched, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                        em.lock(myQmsplan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

                        myQms.setState(QmsTools.STATE_OPEN);
                        myQms.setUser(null);
                        myQms.setActual(null);

                        em.getTransaction().commit();

//                            listQMSPlans.set(listQMSPlans.indexOf(qms.getQmsplan()), myQmsplan);
                        mapQms2Panel.remove(qms);
                        createCP4(myQmsplan);
                        reloadData();
                        buildPanel();

                    } catch (OptimisticLockException ole) {
                        OPDE.warn(ole);
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }

                        OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                        reload();
                    } catch (Exception e) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        OPDE.fatal(e);
                    } finally {
                        em.close();
                    }

                });
                btnEmpty.setEnabled(qms.getQmssched().isActive());
                cptitle.getRight().add(btnEmpty);
            }


            /***
             *               _ _ _  _____         _
             *       ___  __| (_) ||_   _|____  _| |_
             *      / _ \/ _` | | __|| |/ _ \ \/ / __|
             *     |  __/ (_| | | |_ | |  __/>  <| |_
             *      \___|\__,_|_|\__||_|\___/_/\_\\__|
             *
             */
            final JButton btnText = new JButton(SYSConst.icon22chat);
            btnText.setPressedIcon(SYSConst.icon22Pressed);
            btnText.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnText.setContentAreaFilled(false);
            btnText.setBorder(null);
            btnText.setToolTipText(SYSTools.xx("misc.msg.edit.text"));

            btnText.addActionListener(actionEvent -> {
                currentEditor = new DlgYesNo(SYSConst.icon48comment, o -> {

                    if (o != null) {

                        EntityManager em = OPDE.createEM();
                        try {

                            em.getTransaction().begin();
                            Qms myQms = em.merge(qms);
                            myQms.setText(SYSTools.catchNull(o));
                            Qmssched myQmssched = em.merge(myQms.getQmssched());
                            Qmsplan myQmsplan = em.merge(myQms.getQmsplan());

                            em.lock(myQmssched, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                            em.lock(myQmsplan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

                            em.getTransaction().commit();

//                                listQMSPlans.set(listQMSPlans.indexOf(qms.getQmsplan()), myQmsplan);
                            mapQms2Panel.remove(qms);
                            createCP4(myQmsplan);
                            reloadData();
                            buildPanel();

                        } catch (OptimisticLockException ole) {
                            OPDE.warn(ole);
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }

                            OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            reload();
                        } catch (Exception e) {
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            OPDE.fatal(e);
                        } finally {
                            em.close();
                        }
                    }
                    currentEditor = null;
                }, "misc.msg.comment", qms.getText(), null);
                currentEditor.setVisible(true);
            });
            btnText.setEnabled(!qms.isOpen() && qms.getQmssched().isActive());
            if (qms.isOpen()) {
                btnText.setToolTipText(SYSTools.xx("opde.controlling.qms.pnlqmsplan.cant.comment"));
            }
            if (!qms.getQmssched().isActive()) {
                btnText.setToolTipText(SYSTools.xx("misc.msg.inactive"));
            }
            cptitle.getRight().add(btnText);

            /***
             *      _     _         _____ _ _
             *     | |__ | |_ _ __ |  ___(_) | ___  ___
             *     | '_ \| __| '_ \| |_  | | |/ _ \/ __|
             *     | |_) | |_| | | |  _| | | |  __/\__ \
             *     |_.__/ \__|_| |_|_|   |_|_|\___||___/
             *
             */
            final JButton btnFiles = qms.getAttachedFilesConnections().isEmpty() ? new JButton(SYSConst.icon22attach) : new JButton(Integer.toString(qms.getAttachedFilesConnections().size()), SYSConst.icon22greenStar);
            btnFiles.setToolTipText(SYSTools.xx("misc.btnfiles.tooltip"));
            btnFiles.setForeground(Color.BLUE);
            btnFiles.setHorizontalTextPosition(SwingUtilities.CENTER);
            btnFiles.setFont(SYSConst.ARIAL18BOLD);
            btnFiles.setPressedIcon(SYSConst.icon22Pressed);
            btnFiles.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnFiles.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnFiles.setContentAreaFilled(false);
            btnFiles.setBorder(null);

            btnFiles.addActionListener(actionEvent -> {
                Closure fileHandleClosure = o -> {
                    EntityManager em = OPDE.createEM();
//                        final Qms updateQms = em.find(Qms.class, qms.getId());
                    final Qmsplan updateQmsplan = em.find(Qmsplan.class, qms.getQmsplan().getId());
                    em.close();

//                            listQMSPlans.set(listQMSPlans.indexOf(qms.getQmsplan()), updateQmsplan);
                    mapQms2Panel.remove(qms);
                    createCP4(updateQmsplan);
                    currentEditor = null;
                    buildPanel();
                };
                currentEditor = new DlgFiles(qms, fileHandleClosure);
                currentEditor.setVisible(true);
            });
            btnFiles.setEnabled(!qms.isOpen() && OPDE.isFTPworking());
            cptitle.getRight().add(btnFiles);
        }

        cpQMS.setTitleLabelComponent(cptitle.getMain());
        cpQMS.setSlidingDirection(SwingConstants.SOUTH);

        cpQMS.setBackground(GUITools.blend(qms.getQmsplan().getColor(), Color.WHITE, 0.025f));

        cpQMS.setCollapsible(qms.hasText());

        JPanel pnl = new JPanel(new VerticalLayout());

//        final JTextPane textPane = new JTextPane();
//        textPane.setText("test");
//        textPane.setEditable(false);
//        textPane.setContentType("text/html");

        JTextArea txtArea = new JTextArea(qms.getText());
        txtArea.setWrapStyleWord(true);
        txtArea.setLineWrap(true);

        pnl.add(txtArea);
        pnl.setOpaque(false);

        cpQMS.setContentPane(pnl);

        try {
            cpQMS.setCollapsed(true);
        } catch (PropertyVetoException e) {
            OPDE.error(e);
        }

        cpQMS.setHorizontalAlignment(SwingConstants.LEADING);
        cpQMS.setOpaque(false);

        mapQms2Panel.put(qms, cpQMS);

        return cpQMS;

    }


    private JPanel getMenu(final Qmsplan qmsplan) {

        final JPanel pnlMenu = new JPanel(new VerticalLayout());
//        long numQMS = 0l;//DFNTools.getNumDFNs(np);

        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.UPDATE, "opde.controlling")) {


            /***
             *               _ _ _
             *       ___  __| (_) |_
             *      / _ \/ _` | | __|
             *     |  __/ (_| | | |_
             *      \___|\__,_|_|\__|
             *
             */
            JButton btnEdit = GUITools.createHyperlinkButton("misc.commands.edit", SYSConst.icon22edit, null);
            btnEdit.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnEdit.addActionListener(actionEvent -> {
                currentEditor = new DlgQMSPlan(qmsplan, qmsplan1 -> {
                    if (qmsplan1 != null) {
                        EntityManager em = OPDE.createEM();
                        try {
                            em.getTransaction().begin();
                            Qmsplan myQMSPlan = (Qmsplan) em.merge(qmsplan1);
                            em.lock(myQMSPlan, LockModeType.OPTIMISTIC);
                            em.getTransaction().commit();

                            reloadData();
                            createCP4(myQMSPlan);
                            buildPanel();
                        } catch (OptimisticLockException ole) {
                            OPDE.warn(ole);
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }

                            OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            reload();
                        } catch (Exception e) {
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            OPDE.fatal(e);
                        } finally {
                            em.close();
                        }
                    }
                    currentEditor = null;
                });
                currentEditor.setVisible(true);
            });
            btnEdit.setEnabled(qmsplan.isActive());
            pnlMenu.add(btnEdit);
        }

        /***
         *          _      _      _
         *       __| | ___| | ___| |_ ___
         *      / _` |/ _ \ |/ _ \ __/ _ \
         *     | (_| |  __/ |  __/ ||  __/
         *      \__,_|\___|_|\___|\__\___|
         *
         */
        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.DELETE, "opde.controlling")) {  // => ACL_MATRIX
            JButton btnDelete = GUITools.createHyperlinkButton("misc.commands.delete", SYSConst.icon22delete, null);
            btnDelete.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnDelete.addActionListener(actionEvent -> {
                currentEditor = new DlgYesNo(SYSTools.xx("misc.questions.delete1") + "<br/><b>" + qmsplan.getTitle() + "</b><br/>" + SYSTools.xx("misc.questions.delete2"), SYSConst.icon48delete, o -> {
                    if (o.equals(JOptionPane.YES_OPTION)) {
                        EntityManager em = OPDE.createEM();
                        try {
                            em.getTransaction().begin();
                            Qmsplan myQMSPlan = em.merge(qmsplan);
                            em.remove(myQMSPlan);
                            em.getTransaction().commit();


                            // Refresh Display
                            listQMSPlans.remove(qmsplan);
//                                    reload();
                            buildPanel();
                            OPDE.getDisplayManager().addSubMessage(DisplayManager.getSuccessMessage(qmsplan.getTitle(), "deleted"));

                        } catch (OptimisticLockException ole) {
                            OPDE.warn(ole);
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            reload();
                        } catch (Exception e) {
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            OPDE.fatal(e);
                        } finally {
                            em.close();
                            currentEditor = null;
                        }
                    }
                });
                currentEditor.setVisible(true);
            });
            btnDelete.setEnabled(qmsplan.isActive());
            pnlMenu.add(btnDelete);
        }

        /***
         *                _
         *       ___ ___ | | ___  _ __
         *      / __/ _ \| |/ _ \| '__|
         *     | (_| (_) | | (_) | |
         *      \___\___/|_|\___/|_|
         *
         */
        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.UPDATE, internalClassID)) {
            final JButton btnColor = GUITools.createHyperlinkButton("misc.msg.colorset", SYSConst.icon22colorset, null);
            btnColor.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnColor.addActionListener(e -> {
                final JColorChooser clr = new JColorChooser(qmsplan.getColor());
                final JidePopup popup = new JidePopup();
                clr.getSelectionModel().addChangeListener(e1 -> {
                    popup.hidePopup();
                    EntityManager em = OPDE.createEM();
                    try {
                        em.getTransaction().begin();
                        Qmsplan myQMSPlan = em.merge(qmsplan);
                        myQMSPlan.setColor(clr.getColor());
                        em.lock(myQMSPlan, LockModeType.OPTIMISTIC);
                        em.getTransaction().commit();
//                                listQMSPlans.remove(qmsplan);
//                                listQMSPlans.add(myQMSPlan);
                        reload();
                    } catch (OptimisticLockException ole) {
                        OPDE.warn(ole);
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                    } catch (RollbackException ole) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        OPDE.getDisplayManager().addSubMessage(new DisplayMessage(ole.getMessage(), DisplayMessage.IMMEDIATELY));
                    } catch (Exception ex) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        OPDE.fatal(ex);
                    } finally {
                        em.close();
                    }
                });

                popup.setMovable(false);
                popup.getContentPane().setLayout(new BoxLayout(popup.getContentPane(), BoxLayout.LINE_AXIS));
                popup.setPopupType(JidePopup.HEAVY_WEIGHT_POPUP);

                popup.setOwner(btnColor);
                popup.removeExcludedComponent(btnColor);
                popup.setTransient(false);
                popup.getContentPane().add(clr);
                popup.setDefaultFocusComponent(clr);
                GUITools.showPopup(popup, SwingConstants.SOUTH_WEST);
            });
            pnlMenu.add(btnColor);
        }

        return pnlMenu;
    }


}
