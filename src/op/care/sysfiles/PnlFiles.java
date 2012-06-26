/*
 * OffenePflege
 * Copyright (C) 2011 Torsten Löhr
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
 */
package op.care.sysfiles;

import com.jidesoft.pane.CollapsiblePane;
import com.jidesoft.pane.CollapsiblePanes;
import com.jidesoft.popup.JidePopup;
import com.jidesoft.swing.JideBoxLayout;
import entity.Bewohner;
import entity.BewohnerTools;
import entity.files.SYSFiles;
import entity.files.SYSFilesTools;
import op.OPDE;
import op.system.DlgYesNo;
import op.system.FileDrop;
import op.threads.DisplayMessage;
import op.tools.GUITools;
import op.tools.InternalClassACL;
import op.tools.NursingRecordsPanel;
import op.tools.SYSTools;
import org.apache.commons.collections.Closure;
import org.jdesktop.swingx.VerticalLayout;
import tablemodels.TMSYSFiles;
import tablerenderer.RNDHTML;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author tloehr
 */
public class PnlFiles extends NursingRecordsPanel {
    public static final String internalClassID = "nursingrecords.files";
    private JPopupMenu menu;
    private Bewohner bewohner;
    private JScrollPane jspSearch;
    private CollapsiblePanes searchPanes;

    /**
     * Creates new form PnlFiles
     */
    public PnlFiles(Bewohner bewohner, JScrollPane jspSearch) {
        initComponents();
        this.jspSearch = jspSearch;

        initPanel();
        change2Bewohner(bewohner);
    }


    private void initPanel() {
        prepareSearchArea();

    }

    @Override
    public void cleanup() {
        SYSTools.unregisterListeners(menu);
        SYSTools.unregisterListeners(this);
    }

    @Override
    public void change2Bewohner(Bewohner bewohner) {
        this.bewohner = bewohner;
        OPDE.getDisplayManager().setMainMessage(BewohnerTools.getBWLabelText(bewohner));
        reloadTable();
    }

    @Override
    public void reload() {
        reloadTable();
    }

    void reloadTable() {

        EntityManager em = OPDE.createEM();
        Query query = em.createNamedQuery("SYSFiles.findByBWKennung", SYSFiles.class);
        query.setParameter("bewohner", bewohner);
        ArrayList<SYSFiles> files = new ArrayList<SYSFiles>(query.getResultList());
        Collections.sort(files);
        em.close();

        tblFiles.setModel(new TMSYSFiles(files));
        tblFiles.getColumnModel().getColumn(0).setCellRenderer(new RNDHTML());
        tblFiles.getColumnModel().getColumn(1).setCellRenderer(new RNDHTML());
        tblFiles.getColumnModel().getColumn(2).setCellRenderer(new RNDHTML());

        tblFiles.getColumnModel().getColumn(0).setHeaderValue(OPDE.lang.getString(internalClassID + ".tabheader1"));
        tblFiles.getColumnModel().getColumn(1).setHeaderValue(OPDE.lang.getString(internalClassID + ".tabheader2"));
        tblFiles.getColumnModel().getColumn(2).setHeaderValue(OPDE.lang.getString(internalClassID + ".tabheader3"));


        jspFiles.dispatchEvent(new ComponentEvent(jspFiles, ComponentEvent.COMPONENT_RESIZED));
//        tblFiles.getColumnModel().getColumn(2).setCellRenderer(new RNDHTML());
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        pnlMain = new JPanel();
        jspFiles = new JScrollPane();
        tblFiles = new JTable();

        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //======== pnlMain ========
        {
            pnlMain.setLayout(new BoxLayout(pnlMain, BoxLayout.X_AXIS));

            //======== jspFiles ========
            {
                jspFiles.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        jspFilesComponentResized(e);
                    }
                });

                //---- tblFiles ----
                tblFiles.setModel(new DefaultTableModel(
                        new Object[][]{
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null},
                        },
                        new String[]{
                                "Title 1", "Title 2", "Title 3", "Title 4"
                        }
                ));
                tblFiles.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                tblFiles.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        tblFilesMousePressed(e);
                    }
                });
                jspFiles.setViewportView(tblFiles);
            }
            pnlMain.add(jspFiles);
        }
        add(pnlMain);
    }// </editor-fold>//GEN-END:initComponents

    private void tblFilesMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblFilesMousePressed

        Point p = evt.getPoint();
        ListSelectionModel lsm = tblFiles.getSelectionModel();
        Point p2 = evt.getPoint();
        SwingUtilities.convertPointToScreen(p2, tblFiles);
        final Point screenposition = p2;

        boolean singleRowSelected = lsm.getMaxSelectionIndex() == lsm.getMinSelectionIndex();

        final int row = tblFiles.rowAtPoint(p);
        final int col = tblFiles.columnAtPoint(p);
        if (singleRowSelected) {
            lsm.setSelectionInterval(row, row);
        }

        final TMSYSFiles tm = (TMSYSFiles) tblFiles.getModel();
        final SYSFiles sysfile = tm.getRow(row);

        if (evt.isPopupTrigger()) {

            SYSTools.unregisterListeners(menu);
            menu = new JPopupMenu();

            // SELECT
            JMenuItem itemPopupShow = new JMenuItem(OPDE.lang.getString("misc.commands.show"), new ImageIcon(getClass().getResource("/artwork/22x22/bw/viewmag1.png")));
            itemPopupShow.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    SYSFilesTools.handleFile(sysfile, Desktop.Action.OPEN);
                }
            });
            menu.add(itemPopupShow);
//            itemPopupShow.setEnabled(OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.SELECT));


            if (col == TMSYSFiles.COL_DESCRIPTION && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE)) {

                final JMenuItem itemPopupEdit = new JMenuItem(OPDE.lang.getString("misc.commands.edit"), new ImageIcon(getClass().getResource("/artwork/22x22/bw/edit.png")));
                itemPopupEdit.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {

                        final JidePopup popup = new JidePopup();
                        popup.setMovable(false);
                        popup.getContentPane().setLayout(new BoxLayout(popup.getContentPane(), BoxLayout.LINE_AXIS));

                        final JComponent editor = new JTextArea(sysfile.getBeschreibung(), 10, 40);
                        ((JTextArea) editor).setLineWrap(true);
                        ((JTextArea) editor).setWrapStyleWord(true);
                        ((JTextArea) editor).setEditable(true);

                        popup.getContentPane().add(new JScrollPane(editor));
                        final JButton saveButton = new JButton(new ImageIcon(getClass().getResource("/artwork/22x22/apply.png")));
                        saveButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                EntityManager em = OPDE.createEM();
                                try {
                                    em.getTransaction().begin();
                                    popup.hidePopup();
                                    SYSFiles mySysfile = em.merge(sysfile);
                                    mySysfile.setBeschreibung(((JTextArea) editor).getText().trim());
                                    em.getTransaction().commit();
                                    tm.setSYSFile(row, mySysfile);
                                } catch (Exception e) {
                                    em.getTransaction().rollback();
                                    OPDE.fatal(e);
                                } finally {
                                    em.close();
                                }

                            }
                        });

                        saveButton.setHorizontalAlignment(SwingConstants.RIGHT);
                        JPanel pnl = new JPanel(new BorderLayout(10, 10));
                        JScrollPane pnlEditor = new JScrollPane(editor);

                        pnl.add(pnlEditor, BorderLayout.CENTER);
                        JPanel buttonPanel = new JPanel();
                        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
                        buttonPanel.add(saveButton);
                        pnl.setBorder(new EmptyBorder(10, 10, 10, 10));
                        pnl.add(buttonPanel, BorderLayout.SOUTH);

                        popup.setOwner(tblFiles);
                        popup.removeExcludedComponent(tblFiles);
                        popup.getContentPane().add(pnl);
                        popup.setDefaultFocusComponent(editor);

                        popup.showPopup(screenposition.x, screenposition.y);

                    }
                });
                menu.add(itemPopupEdit);
            }


            if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.DELETE)) {
                JMenuItem itemPopupDelete = new JMenuItem(OPDE.lang.getString("misc.commands.delete"), new ImageIcon(getClass().getResource("/artwork/22x22/bw/trashcan_empty.png")));
                itemPopupDelete.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(java.awt.event.ActionEvent evt) {

                        new DlgYesNo(OPDE.lang.getString("misc.questions.delete1") + "<br/><b>" + sysfile.getFilename() + "</b><br/>" + OPDE.lang.getString("misc.questions.delete2"), new ImageIcon(getClass().getResource("/artwork/48x48/bw/trashcan_empty.png")), new Closure() {
                            @Override
                            public void execute(Object o) {
                                SYSFilesTools.deleteFile(sysfile);
                                reloadTable();
                            }
                        });

                    }
                });
                menu.add(itemPopupDelete);
                itemPopupDelete.setEnabled(singleRowSelected);
            }

            menu.show(evt.getComponent(), (int) p.getX(), (int) p.getY());
        } else {
            if (evt.getClickCount() == 2 && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.SELECT)) { // DoppelClick
                SYSFilesTools.handleFile(sysfile, Desktop.Action.OPEN);
            }
        }


    }//GEN-LAST:event_tblFilesMousePressed

    private void jspFilesComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jspFilesComponentResized
        JScrollPane jsp = (JScrollPane) evt.getComponent();
        Dimension dim = jsp.getSize();
        // Größe der Text Spalte im TB ändern.
        // Summe der fixen Spalten  = 210 + ein bisschen
        int textWidth = dim.width - 200;
        tblFiles.getColumnModel().getColumn(0).setPreferredWidth(200);
        tblFiles.getColumnModel().getColumn(1).setPreferredWidth(textWidth / 3 * 2);
        tblFiles.getColumnModel().getColumn(2).setPreferredWidth(textWidth / 3);
//        tblFiles.getColumnModel().getColumn(2).setPreferredWidth(100);
    }//GEN-LAST:event_jspFilesComponentResized

    private void prepareSearchArea() {
        searchPanes = new CollapsiblePanes();
        searchPanes.setLayout(new JideBoxLayout(searchPanes, JideBoxLayout.Y_AXIS));
        jspSearch.setViewportView(searchPanes);

        if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.INSERT)) {
            searchPanes.add(addCommands());
        }
//        searchPanes.add(addFilters());

        searchPanes.addExpansion();

    }

    private CollapsiblePane addCommands() {
        JPanel mypanel = new JPanel();
        mypanel.setLayout(new VerticalLayout());
        mypanel.setBackground(Color.WHITE);

        CollapsiblePane cmdPane = new CollapsiblePane(OPDE.lang.getString(internalClassID));
        cmdPane.setStyle(CollapsiblePane.PLAIN_STYLE);
        cmdPane.setCollapsible(false);

        try {
            cmdPane.setCollapsed(false);
        } catch (PropertyVetoException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        mypanel.add(GUITools.getDropPanel(new FileDrop.Listener() {
            public void filesDropped(java.io.File[] files) {
                java.util.List<SYSFiles> successful = SYSFilesTools.putFiles(files, bewohner);
                if (!successful.isEmpty()) {
                    OPDE.getDisplayManager().addSubMessage(new DisplayMessage(successful.size() + " " + OPDE.lang.getString("misc.msg.Files") + " " + OPDE.lang.getString("misc.msg.added")));
                }
                reloadTable();
            }
        }));


        cmdPane.setContentPane(mypanel);
        return cmdPane;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel pnlMain;
    private JScrollPane jspFiles;
    private JTable tblFiles;
    // End of variables declaration//GEN-END:variables
}
