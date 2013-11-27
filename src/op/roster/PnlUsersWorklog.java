/*
 * Created by JFormDesigner on Thu Aug 15 16:52:39 CEST 2013
 */

package op.roster;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.jidesoft.grid.TableScrollPane;
import com.jidesoft.pane.CollapsiblePane;
import entity.EntityTools;
import entity.Homes;
import entity.HomesTools;
import entity.roster.*;
import entity.system.SYSPropsTools;
import entity.system.Users;
import entity.system.UsersTools;
import op.OPDE;
import op.tools.CleanablePanel;
import op.tools.SYSCalendar;
import op.tools.SYSTools;
import org.joda.time.LocalDate;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Torsten Löhr
 */
public class PnlUsersWorklog extends CleanablePanel {
    public static final String internalClassID = "opde.all.rosters";

    private Map<String, CollapsiblePane> cpMap;
    private Map<String, JPanel> contentmap;
    private TableScrollPane tsp1;
    private ArrayList<Rosters> lstAllRosters;
    private JPopupMenu menu;

    public PnlUsersWorklog() {
        initComponents();
        initPanel();
    }

    private void initPanel() {

        EntityManager em = OPDE.createEM();
        lstAllRosters = new ArrayList<Rosters>(em.createQuery("SELECT r FROM Rosters r ORDER BY r.month ASC").getResultList());
        em.close();

        lstRosters.setModel(SYSTools.list2dlm(lstAllRosters));
        lstRosters.setCellRenderer(RostersTools.getRenderer());

        Users user = EntityTools.find(Users.class, "glaumann");
        Rosters roster = EntityTools.find(Rosters.class, 6l);
        RosterParameters rosterParameters =  RostersTools.getParameters(roster);
        UserContracts userContracts = UsersTools.getContracts(user);

        pnlWorklog.add(new PnlWorkingLogWeek(user, new LocalDate(roster.getMonth()).plusWeeks(1), rosterParameters, userContracts));

    }

    private void lstRostersMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            final FrmRoster frmRoster = OPDE.getMainframe().addRoster((Rosters) lstRosters.getSelectedValue());
            final int pos = lstAllRosters.indexOf(lstRosters.getSelectedValue());
            lstAllRosters.remove(pos);
            lstAllRosters.add(pos, frmRoster.getRoster());
            lstRosters.setModel(SYSTools.list2dlm(lstAllRosters));
            frmRoster.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    lstAllRosters.remove(pos);
                    lstAllRosters.add(pos, frmRoster.getRoster());
                    lstRosters.setModel(SYSTools.list2dlm(lstAllRosters));
                    super.windowClosed(e);
                }
            });
        }
    }

    private void btnNewRosterActionPerformed(ActionEvent e) {
        LocalDate monthToCreate = null;
        String paramsXML = null;

        Homes defaultHome = HomesTools.getAll().get(0);

        if (lstAllRosters.isEmpty()) {
            JComboBox cmbMonth = new JComboBox(SYSCalendar.createMonthList(new LocalDate().minusYears(1).monthOfYear().withMinimumValue(), new LocalDate().monthOfYear().withMaximumValue()));
            final Format monthFormatter = new SimpleDateFormat("MMMM yyyy");
            cmbMonth.setRenderer(new ListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    return new DefaultListCellRenderer().getListCellRendererComponent(list, monthFormatter.format(((LocalDate) value).toDate()), index, isSelected, cellHasFocus);

                }
            });
            cmbMonth.setSelectedItem(SYSCalendar.bom(new LocalDate()));
            JOptionPane.showMessageDialog(this, cmbMonth);
            monthToCreate = new LocalDate(cmbMonth.getSelectedItem());
            paramsXML = RostersTools.DEFAULT_XML;
        } else {
            monthToCreate = new LocalDate(lstAllRosters.get(lstAllRosters.size() - 1).getMonth()).plusMonths(1).dayOfMonth().withMinimumValue();
            paramsXML = lstAllRosters.get(lstAllRosters.size() - 1).getXml();
        }


        EntityManager em = OPDE.createEM();
        em.getTransaction().begin();
        Rosters newRoster = em.merge(new Rosters(monthToCreate, paramsXML));

        // the stats of users with a valid contract for this month are entered here
        HashMap<Users, UserContracts> mapUsers = UsersTools.getUsersWithValidContractsIn(monthToCreate);

        String userlist = "";
        for (Users user : mapUsers.keySet()) {
            OPDE.debug(user);
//            OPDE.debug(mapUsers.get(user).getTargetHoursForMonth(monthToCreate, user));
            em.merge(mapUsers.get(user).getTargetHoursForMonth(monthToCreate, user));
            userlist += user.getUID() + "=" + defaultHome.getEID() + ",";
        }

        if (!userlist.isEmpty()) {
            userlist = userlist.substring(0, userlist.length() - 1);
        }
        // ma liste für plan eintragen. auch mehrfach nennnungen erlauben. vielleicht über sysprops.

        em.getTransaction().commit();
        em.close();


        SYSPropsTools.storeProp("rosterid:" + newRoster.getId(), userlist);

        lstAllRosters.add(newRoster);
        lstRosters.setModel(SYSTools.list2dlm(lstAllRosters));

    }

    private void lstRostersMousePressed(MouseEvent evt) {
        Point p = evt.getPoint();
        final int row = lstRosters.locationToIndex(p);

        lstRosters.setSelectedIndex(row);

        final Rosters selectedRoster = (Rosters) lstRosters.getSelectedValue();

        if (SwingUtilities.isRightMouseButton(evt) && selectedRoster != null && selectedRoster.getOpenedBy() != null && !selectedRoster.getOpenedBy().equals(OPDE.getLogin()) && OPDE.isAdmin()) {

            SYSTools.unregisterListeners(menu);
            menu = new JPopupMenu();

            JMenuItem forceUnlock = new JMenuItem(OPDE.lang.getString("opde.roster.force.unlock"), null);
            forceUnlock.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    EntityManager em = OPDE.createEM();
                    try {
                        em.getTransaction().begin();
                        Rosters myRoster = em.merge(selectedRoster);
                        em.lock(myRoster, LockModeType.OPTIMISTIC);
                        myRoster.setOpenedBy(null);

                        em.getTransaction().commit();
                    } catch (OptimisticLockException ole) {
                        OPDE.debug(ole);
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                    } catch (Exception e) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        OPDE.fatal(e);
                    } finally {
                        em.close();
                    }
                    reload();
                }
            });
            menu.add(forceUnlock);

            if (menu != null) {
                menu.show(evt.getComponent(), (int) p.getX(), (int) p.getY());
            }
        }
    }




    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        tabbedPane1 = new JTabbedPane();
        pnlWorklog = new JPanel();
        panel1 = new JPanel();
        scrollPane1 = new JScrollPane();
        lstRosters = new JList();
        btnNewRoster = new JButton();

        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //======== tabbedPane1 ========
        {

            //======== pnlWorklog ========
            {
                pnlWorklog.setLayout(new BoxLayout(pnlWorklog, BoxLayout.X_AXIS));
            }
            tabbedPane1.addTab("Workinglog", pnlWorklog);

            //======== panel1 ========
            {
                panel1.setLayout(new FormLayout(
                    "default:grow",
                    "default:grow, $lgap, default"));

                //======== scrollPane1 ========
                {

                    //---- lstRosters ----
                    lstRosters.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            lstRostersMouseClicked(e);
                        }
                        @Override
                        public void mousePressed(MouseEvent e) {
                            lstRostersMousePressed(e);
                        }
                    });
                    scrollPane1.setViewportView(lstRosters);
                }
                panel1.add(scrollPane1, CC.xy(1, 1, CC.DEFAULT, CC.FILL));

                //---- btnNewRoster ----
                btnNewRoster.setText("new roster");
                btnNewRoster.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btnNewRosterActionPerformed(e);
                    }
                });
                panel1.add(btnNewRoster, CC.xy(1, 3));
            }
            tabbedPane1.addTab("Roster", panel1);
        }
        add(tabbedPane1);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    @Override
    public void cleanup() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void reload() {
        initPanel();
    }

    @Override
    public String getInternalClassID() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTabbedPane tabbedPane1;
    private JPanel pnlWorklog;
    private JPanel panel1;
    private JScrollPane scrollPane1;
    private JList lstRosters;
    private JButton btnNewRoster;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
