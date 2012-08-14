/*
 * OffenePflege
 * Copyright (C) 2008 Torsten Löhr
 * This program is free software; you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License V2 as published by the Free Software Foundation
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even 
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to 
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 * www.offene-pflege.de
 * ------------------------ 
 * Auf deutsch (freie Übersetzung. Rechtlich gilt die englische Version)
 * Dieses Programm ist freie Software. Sie können es unter den Bedingungen der GNU General Public License, 
 * wie von der Free Software Foundation veröffentlicht, weitergeben und/oder modifizieren, gemäß Version 2 der Lizenz.
 *
 * Die Veröffentlichung dieses Programms erfolgt in der Hoffnung, daß es Ihnen von Nutzen sein wird, aber 
 * OHNE IRGENDEINE GARANTIE, sogar ohne die implizite Garantie der MARKTREIFE oder der VERWENDBARKEIT FÜR EINEN 
 * BESTIMMTEN ZWECK. Details finden Sie in der GNU General Public License.
 *
 * Sie sollten ein Exemplar der GNU General Public License zusammen mit diesem Programm erhalten haben. Falls nicht, 
 * schreiben Sie an die Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA.
 * 
 */
package op.care.dfn;

import com.jidesoft.pane.CollapsiblePane;
import com.jidesoft.pane.CollapsiblePanes;
import com.jidesoft.popup.JidePopup;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.swing.JideButton;
import com.toedter.calendar.JDateChooser;
import entity.info.Resident;
import entity.info.ResidentTools;
import entity.planung.*;
import op.OPDE;
import op.care.planung.massnahmen.PnlSelectIntervention;
import op.threads.DisplayManager;
import op.threads.DisplayMessage;
import op.tools.*;
import org.apache.commons.collections.Closure;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

/**
 * @author root
 */
public class PnlDFN extends NursingRecordsPanel {

    public static final String internalClassID = "nursingrecords.dfn";

    Resident bewohner;

    private boolean initPhase;

    private JScrollPane jspSearch;
    private CollapsiblePanes searchPanes;
    private JDateChooser jdcDatum;
    private JComboBox cmbSchicht;

    private ArrayList<NursingProcess> involvedNPs;
    private HashMap<DFN, CollapsiblePane> dfnCollapsiblePaneHashMap;
    private HashMap<NursingProcess, ArrayList<DFN>> nursingProcessArrayListHashMap;
    private ArrayList<DFN> unassignedDFNArrayList;
    private CollapsiblePane unassignedPane;
    private int MAX_TEXT_LENGTH = 110;

    public PnlDFN(Resident bewohner, JScrollPane jspSearch) {
        initComponents();
        this.jspSearch = jspSearch;
        initPanel();
        switchResident(bewohner);
    }

    private void initPanel() {
        nursingProcessArrayListHashMap = new HashMap<NursingProcess, ArrayList<DFN>>();
        dfnCollapsiblePaneHashMap = new HashMap<DFN, CollapsiblePane>();
        prepareSearchArea();
    }

    @Override
    public void cleanup() {
        jdcDatum.cleanup();
        involvedNPs.clear();
        cpDFN.removeAll();
        dfnCollapsiblePaneHashMap.clear();
        nursingProcessArrayListHashMap.clear();
        SYSTools.unregisterListeners(this);
    }

    @Override
    public void reload() {
        reloadDisplay();
    }


    @Override
    public void switchResident(Resident bewohner) {
        this.bewohner = bewohner;
        OPDE.getDisplayManager().setMainMessage(ResidentTools.getBWLabelText(bewohner));

        initPhase = true;
        jdcDatum.setMinSelectableDate(DFNTools.getMinDatum(bewohner));
        jdcDatum.setDate(new Date());
        initPhase = false;

        reloadDisplay();

    }


    private void reloadDisplay() {
        /***
         *               _                 _ ____  _           _
         *      _ __ ___| | ___   __ _  __| |  _ \(_)___ _ __ | | __ _ _   _
         *     | '__/ _ \ |/ _ \ / _` |/ _` | | | | / __| '_ \| |/ _` | | | |
         *     | | |  __/ | (_) | (_| | (_| | |_| | \__ \ |_) | | (_| | |_| |
         *     |_|  \___|_|\___/ \__,_|\__,_|____/|_|___/ .__/|_|\__,_|\__, |
         *                                              |_|            |___/
         */
        initPhase = true;

        final boolean withworker = true;
        if (withworker) {

            OPDE.getMainframe().setBlocked(true);
            OPDE.getDisplayManager().setProgressBarMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.wait"), -1, 100));

            cpDFN.removeAll();

            SwingWorker worker = new SwingWorker() {

                @Override
                protected Object doInBackground() throws Exception {

                    int progress = 0;
                    if (involvedNPs != null) {
                        involvedNPs.clear();
                    }
                    dfnCollapsiblePaneHashMap.clear();
                    nursingProcessArrayListHashMap.clear();
                    involvedNPs = DFNTools.getInvolvedNPs((byte) (cmbSchicht.getSelectedIndex() - 1), bewohner, jdcDatum.getDate());

                    cpDFN.setLayout(new JideBoxLayout(cpDFN, JideBoxLayout.Y_AXIS));
                    for (NursingProcess np : involvedNPs) {
                        progress++;
                        OPDE.getDisplayManager().setProgressBarMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.wait"), progress, involvedNPs.size()));
                        cpDFN.add(createCollapsiblePanesFor(np));
                    }

                    // this adds unassigned DFNs to the panel
                    CollapsiblePane unassignedPane = createCollapsiblePanesFor((NursingProcess) null);
                    if (unassignedPane != null) {
                        cpDFN.add(unassignedPane);
                    }

                    return null;
                }

                @Override
                protected void done() {
                    cpDFN.addExpansion();
                    cpDFN.repaint();
                    initPhase = false;
                    OPDE.getDisplayManager().setProgressBarMessage(null);
                    OPDE.getMainframe().setBlocked(false);
                }
            };
            worker.execute();

        } else {

            cpDFN.removeAll();
            if (involvedNPs != null) {
                involvedNPs.clear();
            }

            dfnCollapsiblePaneHashMap.clear();
            nursingProcessArrayListHashMap.clear();
            involvedNPs = DFNTools.getInvolvedNPs((byte) (cmbSchicht.getSelectedIndex() - 1), bewohner, jdcDatum.getDate());

            cpDFN.setLayout(new JideBoxLayout(cpDFN, JideBoxLayout.Y_AXIS));
            for (NursingProcess np : involvedNPs) {
                cpDFN.add(createCollapsiblePanesFor(np));
            }

            // this adds unassigned DFNs to the panel
            CollapsiblePane unassignedPane = createCollapsiblePanesFor((NursingProcess) null);
            if (unassignedPane != null) {
                cpDFN.add(unassignedPane);
            }

            cpDFN.addExpansion();
        }
        initPhase = false;
    }


    private void refreshDisplay() {
        cpDFN.removeAll();

        for (NursingProcess np : involvedNPs) {
            cpDFN.add(refreshCollapsiblePanesFor(np));
        }

        // this adds unassigned DFNs to the panel
        unassignedPane = createCollapsiblePanesFor((NursingProcess) null);
        if (unassignedPane != null) {
            cpDFN.add(unassignedPane);
        }

        cpDFN.addExpansion();
    }

    private CollapsiblePane refreshCollapsiblePanesFor(final NursingProcess np) {
        String title = "";
        Color fore, back, backcontent;

        if (np == null) {
            title = OPDE.lang.getString(internalClassID + ".ondemand");
            back = Color.LIGHT_GRAY;
            fore = Color.BLACK;
            backcontent = Color.LIGHT_GRAY;
        } else {
            title = np.getStichwort() + " (" + np.getKategorie().getBezeichnung() + ")";
            back = np.getKategorie().getBackgroundHeader();
            fore = np.getKategorie().getForegroundHeader();
            backcontent = np.getKategorie().getBackgroundContent();
        }

        final CollapsiblePane npPane = new CollapsiblePane(title);

        npPane.setSlidingDirection(SwingConstants.SOUTH);
        npPane.setBackground(back);
        npPane.setForeground(fore);
        npPane.setOpaque(false);

        JPanel npPanel = new JPanel();
        npPanel.setLayout(new VerticalLayout());
        npPanel.setBackground(backcontent);

        if (np == null) {
            if (unassignedDFNArrayList.isEmpty()) {
                return null;
            }
        }

        for (DFN dfn : (np == null ? unassignedDFNArrayList : nursingProcessArrayListHashMap.get(np))) {

            npPanel.add(dfnCollapsiblePaneHashMap.get(dfn));
        }

        npPane.setContentPane(npPanel);
        npPane.setCollapsible(false);
        try {
            npPane.setCollapsed(false);
        } catch (PropertyVetoException e) {
            OPDE.error(e);
        }
        return npPane;
    }

    private CollapsiblePane createCollapsiblePanesFor(final NursingProcess np) {
        String title = "";
        Color fore, back, backcontent;

        if (np == null) {
            title = OPDE.lang.getString(internalClassID + ".ondemand");
            back = Color.LIGHT_GRAY;
            fore = Color.BLACK;
            backcontent = Color.LIGHT_GRAY;
        } else {
            title = "<html>" + (np.isAbgesetzt() ? "<s>" : "") + np.getStichwort() + (np.isAbgesetzt() ? "</s>" : "") + " (" + np.getKategorie().getBezeichnung() + ")" + (np.isAbgesetzt() ? " &rarr;" + DateFormat.getDateInstance().format(np.getBis()) : "") + "</html>";
            back = np.getKategorie().getBackgroundHeader();
            fore = np.getKategorie().getForegroundHeader();
            backcontent = np.getKategorie().getBackgroundContent();
        }

        final CollapsiblePane npPane = new CollapsiblePane(title);

        npPane.setSlidingDirection(SwingConstants.SOUTH);
        npPane.setBackground(back);
        npPane.setForeground(fore);
        npPane.setOpaque(false);

        JPanel npPanel = new JPanel();
        npPanel.setLayout(new VerticalLayout());
        npPanel.setBackground(backcontent);

        if (np == null) {
            unassignedDFNArrayList = DFNTools.getDFNs((byte) (cmbSchicht.getSelectedIndex() - 1), bewohner, jdcDatum.getDate());
            if (unassignedDFNArrayList.isEmpty()) {
                return null;
            }
        } else {
            nursingProcessArrayListHashMap.put(np, DFNTools.getDFNs((byte) (cmbSchicht.getSelectedIndex() - 1), np, jdcDatum.getDate()));
        }

        for (DFN dfn : (np == null ? unassignedDFNArrayList : nursingProcessArrayListHashMap.get(np))) {
            dfnCollapsiblePaneHashMap.put(dfn, createCollapsiblePanesFor(dfn));
            npPanel.add(dfnCollapsiblePaneHashMap.get(dfn));
        }


        npPane.setContentPane(npPanel);
        npPane.setCollapsible(false);
        try {
            npPane.setCollapsed(false);
        } catch (PropertyVetoException e) {
            OPDE.error(e);
        }
        return npPane;
    }

    private CollapsiblePane createCollapsiblePanesFor(final DFN dfn) {
        final CollapsiblePane dfnPane = new CollapsiblePane();
        String fg = SYSConst.html_grey50;
        if (dfn.getNursingProcess() != null) {
            fg = "#" + dfn.getNursingProcess().getKategorie().getFgcontent();
        }

        /***
         *      _   _ _____    _    ____  _____ ____
         *     | | | | ____|  / \  |  _ \| ____|  _ \
         *     | |_| |  _|   / _ \ | | | |  _| | |_) |
         *     |  _  | |___ / ___ \| |_| | |___|  _ <
         *     |_| |_|_____/_/   \_\____/|_____|_| \_\
         *
         */

        JPanel titlePanelleft = new JPanel();
        titlePanelleft.setLayout(new BoxLayout(titlePanelleft, BoxLayout.LINE_AXIS));

        /***
         *      _     _       _    _           _   _                _   _                _
         *     | |   (_)_ __ | | _| |__  _   _| |_| |_ ___  _ __   | | | | ___  __ _  __| | ___ _ __
         *     | |   | | '_ \| |/ / '_ \| | | | __| __/ _ \| '_ \  | |_| |/ _ \/ _` |/ _` |/ _ \ '__|
         *     | |___| | | | |   <| |_) | |_| | |_| || (_) | | | | |  _  |  __/ (_| | (_| |  __/ |
         *     |_____|_|_| |_|_|\_\_.__/ \__,_|\__|\__\___/|_| |_| |_| |_|\___|\__,_|\__,_|\___|_|
         *
         */
        JideButton btnDFN = GUITools.createHyperlinkButton("<html><font color=\"" + fg + "\">" +
                SYSTools.left(dfn.getIntervention().getBezeichnung(), MAX_TEXT_LENGTH) +
                DFNTools.getScheduleText(dfn, " <b>[", "]</b>") +
                ", " + dfn.getMinutes() + " " + OPDE.lang.getString("misc.msg.Minute(s)") + (dfn.getUser() != null ? ", <u>" + dfn.getUser().getUKennung() + "</u>" : "") +
                "</html>", DFNTools.getIcon(dfn), null);

//        title.addMouseListener(GUITools.getHyperlinkStyleMouseAdapter());
        btnDFN.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (!dfn.isOnDemand()) {
            btnDFN.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        dfnPane.setCollapsed(!dfnPane.isCollapsed());
                    } catch (PropertyVetoException e) {
                        OPDE.error(e);
                    }
                }
            });
        }
        titlePanelleft.add(btnDFN);


        JPanel titlePanelright = new JPanel();
        titlePanelright.setLayout(new BoxLayout(titlePanelright, BoxLayout.LINE_AXIS));


        if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE)) {

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
            btnApply.setContentAreaFilled(false);
            btnApply.setBorder(null);
            btnApply.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (dfn.getStatus() == DFNTools.STATE_DONE) {
                        return;
                    }

                    if (DFNTools.isChangeable(dfn)) {
                        EntityManager em = OPDE.createEM();
                        try {
                            em.getTransaction().begin();


                            em.lock(em.merge(bewohner), LockModeType.OPTIMISTIC);
                            DFN myDFN = em.merge(dfn);
                            em.lock(myDFN, LockModeType.OPTIMISTIC);

                            myDFN.setStatus(DFNTools.STATE_DONE);
                            myDFN.setUser(em.merge(OPDE.getLogin().getUser()));
                            myDFN.setIst(new Date());
                            myDFN.setiZeit(SYSCalendar.whatTimeIDIs(new Date()));
                            myDFN.setMdate(new Date());
                            dfnCollapsiblePaneHashMap.put(myDFN, createCollapsiblePanesFor(myDFN));
                            int position = nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).indexOf(dfn);
                            nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).remove(dfn);
                            nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).add(position, myDFN);
                            Collections.sort(nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()));
                            em.getTransaction().commit();
                            refreshDisplay();
                        } catch (OptimisticLockException ole) {
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
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

                    } else {
                        OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString(internalClassID + ".notchangeable")));
                    }
                }
            });
            btnApply.setEnabled(!dfn.isOnDemand());
            titlePanelright.add(btnApply);


            /***
             *      _     _          ____                     _
             *     | |__ | |_ _ __  / ___|__ _ _ __   ___ ___| |
             *     | '_ \| __| '_ \| |   / _` | '_ \ / __/ _ \ |
             *     | |_) | |_| | | | |__| (_| | | | | (_|  __/ |
             *     |_.__/ \__|_| |_|\____\__,_|_| |_|\___\___|_|
             *
             */
            final JButton btnCancel = new JButton(SYSConst.icon22cancel);
            btnCancel.setPressedIcon(SYSConst.icon22cancelPressed);
            btnCancel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnCancel.setContentAreaFilled(false);
            btnCancel.setBorder(null);
//            btnCancel.setToolTipText(OPDE.lang.getString(internalClassID + ".btneval.tooltip"));
            btnCancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (dfn.getStatus() == DFNTools.STATE_REFUSED) {
                        return;
                    }

                    if (DFNTools.isChangeable(dfn)) {
                        EntityManager em = OPDE.createEM();
                        try {
                            em.getTransaction().begin();

                            em.lock(em.merge(bewohner), LockModeType.OPTIMISTIC);
                            DFN myDFN = em.merge(dfn);
                            em.lock(myDFN, LockModeType.OPTIMISTIC);

                            myDFN.setStatus(DFNTools.STATE_REFUSED);
                            myDFN.setUser(em.merge(OPDE.getLogin().getUser()));
                            myDFN.setIst(new Date());
                            myDFN.setiZeit(SYSCalendar.whatTimeIDIs(new Date()));
                            myDFN.setMdate(new Date());
                            dfnCollapsiblePaneHashMap.put(myDFN, createCollapsiblePanesFor(myDFN));
                            int position = nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).indexOf(dfn);
                            nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).remove(dfn);
                            nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).add(position, myDFN);
                            Collections.sort(nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()));

                            em.getTransaction().commit();
                            refreshDisplay();
                        } catch (OptimisticLockException ole) {
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
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

                    } else {
                        OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString(internalClassID + ".notchangeable")));
                    }
                }
            });
            btnCancel.setEnabled(!dfn.isOnDemand());
            titlePanelright.add(btnCancel);

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
//            btnCancel.setToolTipText(OPDE.lang.getString(internalClassID + ".btneval.tooltip"));
            btnEmpty.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (dfn.getStatus() == DFNTools.STATE_OPEN) {
                        return;
                    }

                    if (DFNTools.isChangeable(dfn)) {
                        EntityManager em = OPDE.createEM();
                        try {
                            em.getTransaction().begin();

                            em.lock(em.merge(bewohner), LockModeType.OPTIMISTIC);
                            DFN myDFN = em.merge(dfn);
                            em.lock(myDFN, LockModeType.OPTIMISTIC);

                            // on demand DFNs are deleted if they not wanted anymore
                            if (myDFN.isOnDemand()) {
                                em.remove(myDFN);
                                dfnCollapsiblePaneHashMap.remove(myDFN);
                                unassignedDFNArrayList.remove(myDFN);
                            } else {
                                // the normal DFNs (those assigned to a NursingProcess) are reset to the OPEN state.
                                myDFN.setStatus(DFNTools.STATE_OPEN);
                                myDFN.setUser(null);
                                myDFN.setIst(null);
                                myDFN.setiZeit(null);
                                myDFN.setMdate(new Date());
                                dfnCollapsiblePaneHashMap.put(myDFN, createCollapsiblePanesFor(myDFN));
                                int position = nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).indexOf(dfn);
                                nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).remove(dfn);
                                nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).add(position, myDFN);
                                Collections.sort(nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()));
                            }

                            em.getTransaction().commit();
                            refreshDisplay();
                        } catch (OptimisticLockException ole) {
                            if (em.getTransaction().isActive()) {
                                em.getTransaction().rollback();
                            }
                            if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
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

                    } else {
                        OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString(internalClassID + ".notchangeable")));
                    }
                }
            });
            titlePanelright.add(btnEmpty);


            /***
             *      _     _         __  __ _             _
             *     | |__ | |_ _ __ |  \/  (_)_ __  _   _| |_ ___  ___
             *     | '_ \| __| '_ \| |\/| | | '_ \| | | | __/ _ \/ __|
             *     | |_) | |_| | | | |  | | | | | | |_| | ||  __/\__ \
             *     |_.__/ \__|_| |_|_|  |_|_|_| |_|\__,_|\__\___||___/
             *
             */
            final JButton btnMinutes = new JButton(SYSConst.icon22clock);
            final JidePopup popup = new JidePopup();
            btnMinutes.setPressedIcon(SYSConst.icon22clockPressed);
            btnMinutes.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnMinutes.setContentAreaFilled(false);
            btnMinutes.setBorder(null);
//            btnCancel.setToolTipText(OPDE.lang.getString(internalClassID + ".btneval.tooltip"));
            btnMinutes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (!DFNTools.isChangeable(dfn)) {
                        OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString(internalClassID + ".notchangeable")));
                        return;
                    }
                    final JComboBox cmbMinutes = new JComboBox(SYSCalendar.getMinuteCMBModelForDFNs(new int[]{1, 2, 3, 4, 5, 10, 15, 20, 30, 45, 60, 120, 240, 360}));
                    cmbMinutes.setRenderer(new DefaultListCellRenderer() {
                        @Override
                        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                            Pair<String, Integer> myValue = (Pair<String, Integer>) value;
                            return super.getListCellRendererComponent(list, myValue.getFirst(), index, isSelected, cellHasFocus);
                        }
                    });
                    cmbMinutes.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent ie) {
                            if (ie.getStateChange() == ItemEvent.SELECTED) {
                                popup.hidePopup();
                                EntityManager em = OPDE.createEM();
                                try {
                                    em.getTransaction().begin();

                                    em.lock(em.merge(bewohner), LockModeType.OPTIMISTIC);
                                    DFN myDFN = em.merge(dfn);
                                    em.lock(myDFN, LockModeType.OPTIMISTIC);

                                    myDFN.setMinutes(new BigDecimal(((Pair<String, Integer>) ie.getItem()).getSecond()));
                                    myDFN.setUser(em.merge(OPDE.getLogin().getUser()));
                                    myDFN.setMdate(new Date());

                                    dfnCollapsiblePaneHashMap.put(myDFN, createCollapsiblePanesFor(myDFN));

                                    if (dfn.isOnDemand()) {
                                        int position = unassignedDFNArrayList.indexOf(dfn);
                                        unassignedDFNArrayList.remove(dfn);
                                        unassignedDFNArrayList.add(position, myDFN);
                                    } else {
                                        int position = nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).indexOf(dfn);
                                        nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).remove(dfn);
                                        nursingProcessArrayListHashMap.get(myDFN.getNursingProcess()).add(position, myDFN);
                                    }

                                    em.getTransaction().commit();
                                    refreshDisplay();
                                } catch (OptimisticLockException ole) {
                                    if (em.getTransaction().isActive()) {
                                        em.getTransaction().rollback();
                                    }
                                    if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
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


                            }
                        }
                    });

                    popup.setMovable(false);
                    popup.getContentPane().setLayout(new BoxLayout(popup.getContentPane(), BoxLayout.LINE_AXIS));
                    popup.getContentPane().add(cmbMinutes);
                    popup.setOwner(btnMinutes);
                    popup.removeExcludedComponent(cmbMinutes);
                    popup.setDefaultFocusComponent(cmbMinutes);
                    GUITools.showPopup(popup, SwingConstants.WEST);
                }
            });
            btnMinutes.setEnabled(dfn.getStatus() != DFNTools.STATE_OPEN);
            titlePanelright.add(btnMinutes);


            /***
             *      _     _         ___        __
             *     | |__ | |_ _ __ |_ _|_ __  / _| ___
             *     | '_ \| __| '_ \ | || '_ \| |_ / _ \
             *     | |_) | |_| | | || || | | |  _| (_) |
             *     |_.__/ \__|_| |_|___|_| |_|_|  \___/
             *
             */
            final JButton btnInfo = new JButton(SYSConst.icon22infoblue);
            final JidePopup popupInfo = new JidePopup();
            btnInfo.setPressedIcon(SYSConst.icon22infogreen);
            btnInfo.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnInfo.setContentAreaFilled(false);
            btnInfo.setBorder(null);
            final JTextPane txt = new JTextPane();
            txt.setContentType("text/html");
            txt.setEditable(false);

            popupInfo.setMovable(false);
            popupInfo.getContentPane().setLayout(new BoxLayout(popupInfo.getContentPane(), BoxLayout.LINE_AXIS));
            popupInfo.getContentPane().add(new JScrollPane(txt));
            popupInfo.removeExcludedComponent(txt);
            popupInfo.setDefaultFocusComponent(txt);

//            btnCancel.setToolTipText(OPDE.lang.getString(internalClassID + ".btneval.tooltip"));
            btnInfo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    popupInfo.setOwner(btnInfo);
                    txt.setText(SYSTools.toHTMLForScreen(dfn.getInterventionSchedule().getBemerkung()));
                    GUITools.showPopup(popupInfo, SwingConstants.CENTER);
                }
            });

            btnInfo.setEnabled(!dfn.isOnDemand() && !SYSTools.catchNull(dfn.getInterventionSchedule().getBemerkung()).isEmpty());
            titlePanelright.add(btnInfo);
        }


        titlePanelleft.setOpaque(false);
        titlePanelright.setOpaque(false);
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);

        titlePanel.setLayout(new GridBagLayout());
        ((GridBagLayout) titlePanel.getLayout()).columnWidths = new int[]{0, 80};
        ((GridBagLayout) titlePanel.getLayout()).columnWeights = new double[]{1.0, 1.0};

        titlePanel.add(titlePanelleft, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 5), 0, 0));

        titlePanel.add(titlePanelright, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 0), 0, 0));

        dfnPane.setTitleLabelComponent(titlePanel);
        dfnPane.setSlidingDirection(SwingConstants.SOUTH);
//        OPDE.debug("Dimension: " + dfnPane.getPreferredSize());

        /***
         *       ___ ___  _  _ _____ ___ _  _ _____
         *      / __/ _ \| \| |_   _| __| \| |_   _|
         *     | (_| (_) | .` | | | | _|| .` | | |
         *      \___\___/|_|\_| |_| |___|_|\_| |_|
         *
         */
        if (dfn.getNursingProcess() != null) {
            JTextPane contentPane = new JTextPane();
            contentPane.setContentType("text/html");
            contentPane.setText(SYSTools.toHTML(NursingProcessTools.getAsHTML(dfn.getNursingProcess(), false)));
            dfnPane.setContentPane(contentPane);
            dfnPane.setBackground(dfn.getNursingProcess().getKategorie().getBackgroundContent());
            dfnPane.setForeground(dfn.getNursingProcess().getKategorie().getForegroundContent());
        }
        try {
            dfnPane.setCollapsed(true);
        } catch (PropertyVetoException e) {
            OPDE.error(e);
        }
        dfnPane.setCollapsible(dfn.getNursingProcess() != null);
        dfnPane.setHorizontalAlignment(SwingConstants.LEADING);
        dfnPane.setOpaque(false);
        return dfnPane;
    }

    private void prepareSearchArea() {
        searchPanes = new CollapsiblePanes();
        searchPanes.setLayout(new JideBoxLayout(searchPanes, JideBoxLayout.Y_AXIS));
        jspSearch.setViewportView(searchPanes);

        JPanel mypanel = new JPanel();
        mypanel.setLayout(new VerticalLayout());
        mypanel.setBackground(Color.WHITE);

        CollapsiblePane searchPane = new CollapsiblePane(OPDE.lang.getString(internalClassID));
        searchPane.setStyle(CollapsiblePane.PLAIN_STYLE);
        searchPane.setCollapsible(false);

        try {
            searchPane.setCollapsed(false);
        } catch (PropertyVetoException e) {
            OPDE.error(e);
        }

        GUITools.addAllComponents(mypanel, addCommands());
        GUITools.addAllComponents(mypanel, addFilters());

        searchPane.setContentPane(mypanel);

        searchPanes.add(searchPane);
        searchPanes.addExpansion();

    }

    private java.util.List<Component> addFilters() {
        java.util.List<Component> list = new ArrayList<Component>();

        String[] strs = GUITools.getLocalizedMessages(new String[]{"misc.msg.everything", internalClassID + ".shift.veryearly", internalClassID + ".shift.early", internalClassID + ".shift.late", internalClassID + ".shift.verylate"});

        cmbSchicht = new JComboBox(new DefaultComboBoxModel(strs));
        cmbSchicht.setFont(new Font("Arial", Font.PLAIN, 14));
        cmbSchicht.setSelectedIndex((int) SYSCalendar.whatShiftIs(new Date()) + 1);
        cmbSchicht.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (!initPhase) {
                    reloadDisplay();
                }
            }
        });
        list.add(cmbSchicht);

        jdcDatum = new JDateChooser(new Date());
        jdcDatum.setFont(new Font("Arial", Font.PLAIN, 14));

        jdcDatum.setBackground(Color.WHITE);
        jdcDatum.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (initPhase) {
                    return;
                }
                if (evt.getPropertyName().equals("date")) {
                    reloadDisplay();
                }
            }
        });
        list.add(jdcDatum);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setLayout(new HorizontalLayout(5));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JButton homeButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_start.png")));
        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jdcDatum.setDate(jdcDatum.getMinSelectableDate());
            }
        });
        homeButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_start_pressed.png")));
        homeButton.setBorder(null);
        homeButton.setBorderPainted(false);
        homeButton.setOpaque(false);
        homeButton.setContentAreaFilled(false);
        homeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton backButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_back.png")));
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jdcDatum.setDate(SYSCalendar.addDate(jdcDatum.getDate(), -1));
            }
        });
        backButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_back_pressed.png")));
        backButton.setBorder(null);
        backButton.setBorderPainted(false);
        backButton.setOpaque(false);
        backButton.setContentAreaFilled(false);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));


        JButton fwdButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_play.png")));
        fwdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jdcDatum.setDate(SYSCalendar.addDate(jdcDatum.getDate(), 1));
            }
        });
        fwdButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_play_pressed.png")));
        fwdButton.setBorder(null);
        fwdButton.setBorderPainted(false);
        fwdButton.setOpaque(false);
        fwdButton.setContentAreaFilled(false);
        fwdButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton endButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_end.png")));
        endButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jdcDatum.setDate(new Date());
            }
        });
        endButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_end_pressed.png")));
        endButton.setBorder(null);
        endButton.setBorderPainted(false);
        endButton.setOpaque(false);
        endButton.setContentAreaFilled(false);
        endButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));


        buttonPanel.add(homeButton);
        buttonPanel.add(backButton);
        buttonPanel.add(fwdButton);
        buttonPanel.add(endButton);

        list.add(buttonPanel);

        return list;
    }


    private java.util.List<Component> addCommands() {

        java.util.List<Component> list = new ArrayList<Component>();

        /***
         *      _     _            _       _     _
         *     | |__ | |_ _ __    / \   __| | __| |
         *     | '_ \| __| '_ \  / _ \ / _` |/ _` |
         *     | |_) | |_| | | |/ ___ \ (_| | (_| |
         *     |_.__/ \__|_| |_/_/   \_\__,_|\__,_|
         *
         */
        if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.INSERT)) {
            final JideButton btnAdd = GUITools.createHyperlinkButton(OPDE.lang.getString(internalClassID + ".btnadd"), new ImageIcon(getClass().getResource("/artwork/22x22/bw/add.png")), null);
            btnAdd.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    final JidePopup popup = new JidePopup();
                    popup.setMovable(false);
                    PnlSelectIntervention pnl = new PnlSelectIntervention(new Closure() {
                        @Override
                        public void execute(Object o) {
                            popup.hidePopup();
                            if (o != null) {
                                Object[] objects = (Object[]) o;
                                EntityManager em = OPDE.createEM();
                                try {
                                    em.getTransaction().begin();
                                    em.lock(em.merge(bewohner), LockModeType.OPTIMISTIC);

                                    for (Object obj : objects) {
                                        Intervention intervention = em.merge((Intervention) obj);
                                        DFN dfn = em.merge(new DFN(bewohner, intervention));

                                        dfnCollapsiblePaneHashMap.put(dfn, createCollapsiblePanesFor(dfn));
                                        unassignedDFNArrayList.add(dfn);
                                        Collections.sort(unassignedDFNArrayList);
                                    }

                                    em.getTransaction().commit();
                                    refreshDisplay();
                                } catch (OptimisticLockException ole) {
                                    if (em.getTransaction().isActive()) {
                                        em.getTransaction().rollback();
                                    }
                                    if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
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
                            }
                        }
                    });
                    popup.getContentPane().setLayout(new BoxLayout(popup.getContentPane(), BoxLayout.LINE_AXIS));
                    popup.getContentPane().add(pnl);
                    popup.setOwner(btnAdd);
                    popup.removeExcludedComponent(pnl);
                    popup.setDefaultFocusComponent(pnl);
                    GUITools.showPopup(popup, SwingConstants.EAST);
                }
            });
            list.add(btnAdd);

        }

        return list;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jspDFN = new JScrollPane();
        cpDFN = new CollapsiblePanes();

        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //======== jspDFN ========
        {
            jspDFN.setBorder(new BevelBorder(BevelBorder.RAISED));

            //======== cpDFN ========
            {
                cpDFN.setLayout(new BoxLayout(cpDFN, BoxLayout.X_AXIS));
            }
            jspDFN.setViewportView(cpDFN);
        }
        add(jspDFN);
    }// </editor-fold>//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JScrollPane jspDFN;
    private CollapsiblePanes cpDFN;
    // End of variables declaration//GEN-END:variables
}
