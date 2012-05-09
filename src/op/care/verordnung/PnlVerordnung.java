/*
 * OffenePflege
 * Copyright (C) 2006-2012 Torsten Löhr
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
package op.care.verordnung;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.jidesoft.pane.CollapsiblePane;
import com.jidesoft.pane.CollapsiblePanes;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.swing.JideButton;
import entity.Bewohner;
import entity.BewohnerTools;
import entity.EntityTools;
import entity.system.SYSPropsTools;
import entity.verordnungen.*;
import op.OPDE;
import op.care.berichte.DlgBericht;
import op.care.med.vorrat.DlgBestandAbschliessen;
import op.care.med.vorrat.DlgBestandAnbrechen;
import op.threads.DisplayMessage;
import op.tools.*;
import org.apache.commons.collections.Closure;
import org.jdesktop.swingx.VerticalLayout;
import tablemodels.TMVerordnung;
import tablerenderer.RNDHTML;

import javax.persistence.EntityManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author tloehr
 */
public class PnlVerordnung extends NursingRecordsPanel {

    public static final String internalClassID = "nursingrecords.prescription";

    private Bewohner bewohner;
    //    private JFrame parent;
    //    private boolean readOnly = false;
    private JPopupMenu menu;

    private JScrollPane jspSearch;
    private CollapsiblePanes searchPanes;
    private JCheckBox cbAbgesetzt;
    private boolean initPhase;


    /**
     * Dieser Actionlistener wird gebraucht, damit die einzelnen Menüpunkte des Kontextmenüs, nachdem sie
     * aufgerufen wurden, einen reloadTable() auslösen können.
     */
//    private ActionListener standardActionListener;

    /**
     * Creates new form PnlVerordnung
     */
    public PnlVerordnung(Bewohner bewohner, JScrollPane jspSearch) {
        initPhase = true;
        this.jspSearch = jspSearch;
        initComponents();
        prepareSearchArea();
        this.bewohner = bewohner;
        loadTable();
        initPhase = false;
    }

    @Override
    public void change2Bewohner(Bewohner bewohner) {
        this.bewohner = bewohner;


//        BewohnerTools.setBWLabel(lblBW, bewohner);

//        btnNew.setEnabled(OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.INSERT));
//        btnBuchen.setEnabled(false);
//        btnVorrat.setEnabled(false);
//        btnPrint.setEnabled(false);

//        standardActionListener = new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                reloadTable();
//            }
//        };

        OPDE.debug(BewohnerTools.getBWLabelText(bewohner));
        OPDE.getDisplayManager().setMainMessage(BewohnerTools.getBWLabelText(bewohner));
        reloadTable();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jspVerordnung = new JScrollPane();
        tblVerordnung = new JTable();

        //======== this ========
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== jspVerordnung ========
        {
            jspVerordnung.setToolTipText("");
            jspVerordnung.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    jspVerordnungComponentResized(e);
                }
            });

            //---- tblVerordnung ----
            tblVerordnung.setModel(new DefaultTableModel(
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
            tblVerordnung.setToolTipText(null);
            tblVerordnung.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    tblVerordnungMousePressed(e);
                }
            });
            jspVerordnung.setViewportView(tblVerordnung);
        }
        add(jspVerordnung, CC.xy(1, 1));
    }// </editor-fold>//GEN-END:initComponents

    private void btnStellplanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStellplanActionPerformed
        printStellplan();
    }//GEN-LAST:event_btnStellplanActionPerformed

    private void btnPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintActionPerformed
        printVerordnungen(null);
    }//GEN-LAST:event_btnPrintActionPerformed

    private void printVerordnungen(int[] sel) {
        try {
            // Create temp file.
            File temp = File.createTempFile("verordnungen", ".html");

            // Delete temp file when program exits.
            temp.deleteOnExit();

            // Write to temp file
//            BufferedWriter out = new BufferedWriter(new FileWriter(temp));

//            TMVerordnung tm = new TMVerordnung(bwkennung, cbAbgesetzt.isSelected(), cbMedi.isSelected(),
//                    cbOhneMedi.isSelected(), cbBedarf.isSelected(), cbRegel.isSelected(), false);
            List<Verordnung> listVerordnung = ((TMVerordnung) tblVerordnung.getModel()).getVordnungenAt(sel);

            String html = SYSTools.htmlUmlautConversion(VerordnungTools.getVerordnungenAsHTML(listVerordnung));

//            out.write(SYSTools.addHTMLTitle(html, BewohnerTools.getBWLabelText(bewohner), true));

//            out.close();

            SYSPrint.print(html, true);

//            SYSPrint.handleFile(parent, temp.getAbsolutePath(), Desktop.Action.OPEN);
        } catch (IOException e) {
            new DlgException(e);
        }

    }

    private void printStellplan() {

        try {
            // Create temp file.
            File temp = File.createTempFile("stellplan", ".html");

            // Delete temp file when program exits.
            temp.deleteOnExit();

            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            String html = SYSTools.htmlUmlautConversion(VerordnungTools.getStellplanAsHTML(bewohner.getStation().getEinrichtung()));

            out.write(html);

            out.close();
            SYSPrint.handleFile(temp.getAbsolutePath(), Desktop.Action.OPEN);
        } catch (IOException e) {
            new DlgException(e);
        }

    }

    private void tblVerordnungMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblVerordnungMousePressed
        Point p = evt.getPoint();

        final ListSelectionModel lsm = tblVerordnung.getSelectionModel();
        boolean singleRowSelected = lsm.getMaxSelectionIndex() == lsm.getMinSelectionIndex();

        if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {

            int row = tblVerordnung.rowAtPoint(p);
            lsm.setSelectionInterval(row, row);
        }

        final List<Verordnung> selection = ((TMVerordnung) tblVerordnung.getModel()).getVordnungenAt(tblVerordnung.getSelectedRows());

        // Kontext Menü
        if (singleRowSelected && evt.isPopupTrigger()) {

            Verordnung ver = null;

            EntityManager em = OPDE.createEM();
            boolean readOnly = false;
//            try {
//                em.getTransaction().begin();
//                ver = em.merge(selection.get(0));
//                em.lock(ver, LockModeType.PESSIMISTIC_WRITE);
//                readOnly = false;
//                em.getTransaction().commit();
//            } catch (Exception e) {
//                OPDE.debug(e);
//                em.getTransaction().rollback();
//                readOnly = true;
//            } finally {
//                em.close();
//            }
//
            final Verordnung verordnung = selection.get(0);

            long num = BHPTools.getNumBHPs(verordnung);
            boolean editAllowed = !readOnly && num == 0;
            boolean changeAllowed = !readOnly && !verordnung.isBedarf() && !verordnung.isAbgesetzt() && num > 0;
            boolean absetzenAllowed = !readOnly && !verordnung.isAbgesetzt();
            boolean deleteAllowed = !readOnly && num == 0;

            SYSTools.unregisterListeners(menu);
            menu = new JPopupMenu();

            JMenuItem itemPopupEdit = new JMenuItem("Korrigieren");
            itemPopupEdit.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    long numVerKennung = VerordnungTools.getNumVerodnungenMitGleicherKennung(verordnung);
                    int status = numVerKennung == 1 ? DlgVerordnung.EDIT_MODE : DlgVerordnung.EDIT_OF_CHANGE_MODE;

                    OPDE.showJDialogAsSheet(new DlgVerordnung(verordnung, status, new Closure() {
                        @Override
                        public void execute(Object verordnung) {
                            if (verordnung != null) {
                                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Verordnung korrigiert", 2));
                                reloadTable();
                            }
                            OPDE.hideSheet();
                        }
                    }));
                }
            });

            menu.add(itemPopupEdit);
            itemPopupEdit.setEnabled(editAllowed && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE));
            //ocs.setEnabled(this, "itemPopupEditText", itemPopupEditText, !readOnly && status > 0 && changeable);
            // -------------------------------------------------
            JMenuItem itemPopupChange = new JMenuItem("Verändern");
            itemPopupChange.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {

                    OPDE.showJDialogAsSheet(new DlgVerordnung(verordnung, DlgVerordnung.CHANGE_MODE, new Closure() {
                        @Override
                        public void execute(Object verordnung) {
                            if (verordnung != null) {
                                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Verordnung geändert", 2));
                                reloadTable();
                            }
                            OPDE.hideSheet();
                        }
                    }));
                }
            });
            menu.add(itemPopupChange);
            itemPopupChange.setEnabled(changeAllowed && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE));
            // -------------------------------------------------
            JMenuItem itemPopupQuit = new JMenuItem("Absetzen");
            itemPopupQuit.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {

                    OPDE.showJDialogAsSheet(new DlgAbsetzen(verordnung, new Closure() {
                        @Override
                        public void execute(Object verordnung) {
                            if (verordnung != null) {
                                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Verordnung abgesetzt", 2));
                                reloadTable();
                            }
                            OPDE.hideSheet();
                        }
                    }));

//                    new DlgAbsetzen(parent, tblVerordnung.getModel().getValueAt(tblVerordnung.getSelectedRow(), TMVerordnung.COL_MSSN).toString(), verordnung);
//                    reloadTable();
                }
            });
            menu.add(itemPopupQuit);
            itemPopupQuit.setEnabled(absetzenAllowed && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE));
            // -------------------------------------------------
            JMenuItem itemPopupDelete = new JMenuItem("Löschen");
            itemPopupDelete.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    JOptionPane pane = new JOptionPane("Soll die Verordnung wirklich gelöscht werden.", JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,new ImageIcon(getClass().getResource("/artwork/48x48/trashcan_empty.png")));
                    pane.addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                            if (propertyChangeEvent.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                                if (propertyChangeEvent.getNewValue().equals(JOptionPane.YES_OPTION)) {
                                    EntityTools.delete(verordnung);
                                    OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Verordnung gelöscht", 2));
                                    reloadTable();
                                }
                                OPDE.hideSheet();
                            }
                        }
                    });
                    OPDE.showJDialogAsSheet(pane.createDialog(""));
                }
            });
            menu.add(itemPopupDelete);

            itemPopupDelete.setEnabled(deleteAllowed && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.DELETE));
//            itemPopupDelete.setEnabled(true);

            if (verordnung.hasMedi()) {
                menu.add(new JSeparator());

                final MedBestand bestandImAnbruch = MedBestandTools.getBestandImAnbruch(DarreichungTools.getVorratZurDarreichung(bewohner, verordnung.getDarreichung()));
                boolean bestandAbschliessenAllowed = !readOnly && bestandImAnbruch != null && !bestandImAnbruch.hasNextBestand();
                boolean bestandAnbrechenAllowed = !readOnly && bestandImAnbruch == null;

                JMenuItem itemPopupCloseBestand = new JMenuItem("Bestand abschließen");
                itemPopupCloseBestand.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        OPDE.showJDialogAsSheet(new DlgBestandAbschliessen(bestandImAnbruch, new Closure() {
                            @Override
                            public void execute(Object o) {
                                reloadTable();
                                OPDE.hideSheet();
                            }
                        }));
                    }
                });
                menu.add(itemPopupCloseBestand);
                itemPopupCloseBestand.setEnabled(bestandAbschliessenAllowed && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE));

                JMenuItem itemPopupOpenBestand = new JMenuItem("Bestand anbrechen");
                itemPopupOpenBestand.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        new DlgBestandAnbrechen(new JFrame(), verordnung.getDarreichung(), verordnung.getBewohner());
                    }
                });
                menu.add(itemPopupOpenBestand);
                itemPopupOpenBestand.setEnabled(bestandAnbrechenAllowed && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE));
            }
            menu.add(new JSeparator());

            JMenuItem itemPopupPrint = new JMenuItem("Markierte Verordnungen drucken");
            itemPopupPrint.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    int[] sel = tblVerordnung.getSelectedRows();
                    printVerordnungen(sel);
                }
            });
            menu.add(itemPopupPrint);

//            if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.SELECT) && !verordnung.isAbgesetzt() && singleRowSelected) {
//                menu.add(new JSeparator());
//                menu.add(SYSFilesTools.getSYSFilesContextMenu(parent, verordnung, standardActionListener));
//            }
//
//            if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.SELECT) && !verordnung.isAbgesetzt() && singleRowSelected) {
//                menu.add(new JSeparator());
//                menu.add(VorgaengeTools.getVorgangContextMenu(parent, verordnung, bewohner, standardActionListener));
//            }


            menu.add(new JSeparator());
            JMenuItem itemPopupInfo = new JMenuItem("Infos anzeigen");
            itemPopupInfo.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    final MedBestand bestandImAnbruch = MedBestandTools.getBestandImAnbruch(DarreichungTools.getVorratZurDarreichung(bewohner, verordnung.getDarreichung()));

                    long dafid = 0;
                    String message = "VerID: " + verordnung.getVerid();
                    if (bestandImAnbruch != null) {
                        BigDecimal apv = MedBestandTools.getAPVperBW(bestandImAnbruch.getVorrat());
                        BigDecimal apvBest = bestandImAnbruch.getApv();
                        message += "  VorID: " + bestandImAnbruch.getVorrat().getVorID() + "  DafID: " + dafid + "  APV: " + apv + "  APV (Bestand): " + apvBest;
                    }

                    OPDE.getDisplayManager().addSubMessage(new DisplayMessage(message, 10));
                }
            });
            itemPopupInfo.setEnabled(true);
            menu.add(itemPopupInfo);


            menu.show(evt.getComponent(), (int) p.getX(), (int) p.getY());
        }
    }//GEN-LAST:event_tblVerordnungMousePressed

    private void btnVorratActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVorratActionPerformed
//        new DlgVorrat(this.parent, bewohner);
//        reloadTable();
    }//GEN-LAST:event_btnVorratActionPerformed

    private void btnBuchenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuchenActionPerformed
//        new DlgBestand(parent, bewohner, "");
//        reloadTable();
    }//GEN-LAST:event_btnBuchenActionPerformed

    private void jspVerordnungComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jspVerordnungComponentResized
        JScrollPane jsp = (JScrollPane) evt.getComponent();
        Dimension dim = jsp.getSize();

        TableColumnModel tcm1 = tblVerordnung.getColumnModel();

        tcm1.getColumn(TMVerordnung.COL_MSSN).setPreferredWidth(dim.width / 5);  // 1/5 tel der Gesamtbreite
        tcm1.getColumn(TMVerordnung.COL_Dosis).setPreferredWidth(dim.width / 5 * 3);  // 3/5 tel der Gesamtbreite
        tcm1.getColumn(TMVerordnung.COL_Hinweis).setPreferredWidth(dim.width / 5);  // 1/5 tel der Gesamtbreite
        tcm1.getColumn(0).setHeaderValue("Medikament / Massnahme");
        tcm1.getColumn(1).setHeaderValue("Dosierung / Häufigkeit");
        tcm1.getColumn(2).setHeaderValue("Hinweise");

    }//GEN-LAST:event_jspVerordnungComponentResized

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed

    }//GEN-LAST:event_btnNewActionPerformed

    public void cleanup() {
        SYSTools.unregisterListeners(this);
//        SYSRunningClassesTools.endModule(myRunningClass);
    }

    private void loadTable() {

        tblVerordnung.setModel(new TMVerordnung(bewohner, cbAbgesetzt.isSelected(), true));
        tblVerordnung.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

//        btnBuchen.setEnabled(OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE));
//        btnVorrat.setEnabled(OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE));
//        btnPrint.setEnabled(tblVerordnung.getModel().getRowCount() > 0 && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.PRINT));

        jspVerordnung.dispatchEvent(new ComponentEvent(jspVerordnung, ComponentEvent.COMPONENT_RESIZED));
        tblVerordnung.getColumnModel().getColumn(0).setCellRenderer(new RNDHTML());
        tblVerordnung.getColumnModel().getColumn(1).setCellRenderer(new RNDHTML());
        tblVerordnung.getColumnModel().getColumn(2).setCellRenderer(new RNDHTML());
//        tblVerordnung.getColumnModel().getColumn(3).setCellRenderer(new RNDHTML());
//        tblVerordnung.getColumnModel().getColumn(4).setCellRenderer(new RNDHTML());
    }

    private void reloadTable() {
        if (initPhase) return;
        TMVerordnung tm = (TMVerordnung) tblVerordnung.getModel();
        tm.reload(bewohner, cbAbgesetzt.isSelected());
    }


    private void prepareSearchArea() {
        searchPanes = new CollapsiblePanes();
        searchPanes.setLayout(new JideBoxLayout(searchPanes, JideBoxLayout.Y_AXIS));
        jspSearch.setViewportView(searchPanes);


        searchPanes.add(addCommands());
        searchPanes.add(addFilter());

        searchPanes.addExpansion();

    }

    private CollapsiblePane addFilter() {
        JPanel labelPanel = new JPanel();
        labelPanel.setBackground(Color.WHITE);
        labelPanel.setLayout(new VerticalLayout());

        CollapsiblePane panelFilter = new CollapsiblePane("Filter");
        panelFilter.setStyle(CollapsiblePane.PLAIN_STYLE);
        panelFilter.setCollapsible(false);


        cbAbgesetzt = new JCheckBox("Abgesetzte Verordnungen anzeigen");
        cbAbgesetzt.addMouseListener(GUITools.getHyperlinkStyleMouseAdapter());
        cbAbgesetzt.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SYSPropsTools.storeState(internalClassID + ":cbAbgesetzt", cbAbgesetzt);
                reloadTable();
            }
        });

        labelPanel.add(cbAbgesetzt);
        SYSPropsTools.restoreState(internalClassID + ":cbAbgesetzt", cbAbgesetzt);

        panelFilter.setContentPane(labelPanel);

        return panelFilter;
    }

    private CollapsiblePane addCommands() {
        JPanel mypanel = new JPanel();
        mypanel.setLayout(new VerticalLayout());
        mypanel.setBackground(Color.WHITE);

        CollapsiblePane searchPane = new CollapsiblePane("Verordnungen");
        searchPane.setStyle(CollapsiblePane.PLAIN_STYLE);
        searchPane.setCollapsible(false);

        try {
            searchPane.setCollapsed(false);
        } catch (PropertyVetoException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.INSERT)) {
            JideButton addButton = GUITools.createHyperlinkButton("Neue Verordnung eingeben", new ImageIcon(getClass().getResource("/artwork/22x22/bw/add.png")), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    OPDE.showJDialogAsSheet(new DlgVerordnung(new Verordnung(bewohner), DlgVerordnung.NEW_MODE, new Closure() {
                        @Override
                        public void execute(Object verordnung) {
                            if (verordnung != null) {
                                reloadTable();
                            }
                            OPDE.hideSheet();
                            OPDE.getDisplayManager().clearSubMessages();
                        }
                    }));

//                    OPDE.showJDialogAsSheet(new DlgBericht(bewohner, );
                }
            });
            mypanel.add(addButton);
        }

        if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE)) {
            JideButton buchenButton = GUITools.createHyperlinkButton("Medikamente einbuchen", new ImageIcon(getClass().getResource("/artwork/22x22/shetaddrow.png")), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    OPDE.showJDialogAsSheet(new DlgBericht(bewohner, new Closure() {
                        @Override
                        public void execute(Object bericht) {
                            if (bericht != null) {
                                EntityTools.persist(bericht);
                                reloadTable();
                            }

                            OPDE.hideSheet();
                        }
                    }));
                }
            });
            mypanel.add(buchenButton);
        }

        if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE)) {
            JideButton vorratButton = GUITools.createHyperlinkButton("Vorräte bearbeiten", new ImageIcon(getClass().getResource("/artwork/22x22/sheetremocolums.png")), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    OPDE.showJDialogAsSheet(new DlgBericht(bewohner, new Closure() {
                        @Override
                        public void execute(Object bericht) {
                            if (bericht != null) {
                                EntityTools.persist(bericht);
                                reloadTable();
                            }
                            OPDE.hideSheet();
                        }
                    }));
                }
            });
            mypanel.add(vorratButton);
        }

        if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.PRINT)) {
            JideButton printButton = GUITools.createHyperlinkButton("Verordnungen drucken", new ImageIcon(getClass().getResource("/artwork/22x22/bw/printer.png")), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
//                    SYSPrint.print(new JFrame(), SYSTools.htmlUmlautConversion(op.care.DBHandling.getUeberleitung(bewohner, false, false, cbMedi.isSelected(), cbBilanz.isSelected(), cbBerichte.isSelected(), true, false, false, false, cbBWInfo.isSelected())), false);
                }
            });
            mypanel.add(printButton);
        }

        if (OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.PRINT)) {
            JideButton printButton = GUITools.createHyperlinkButton("Stellplan drucken", new ImageIcon(getClass().getResource("/artwork/22x22/bw/printer.png")), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
//                    SYSPrint.print(new JFrame(), SYSTools.htmlUmlautConversion(op.care.DBHandling.getUeberleitung(bewohner, false, false, cbMedi.isSelected(), cbBilanz.isSelected(), cbBerichte.isSelected(), true, false, false, false, cbBWInfo.isSelected())), false);
                }
            });
            mypanel.add(printButton);
        }

        searchPane.setContentPane(mypanel);
        return searchPane;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JScrollPane jspVerordnung;
    private JTable tblVerordnung;
    // End of variables declaration//GEN-END:variables


}
