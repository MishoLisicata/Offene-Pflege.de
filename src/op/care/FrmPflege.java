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
package op.care;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import entity.Bewohner;
import entity.BewohnerTools;
import entity.Stationen;
import entity.StationenTools;
import op.OPDE;
import op.care.berichte.PnlBerichte;
import op.care.bhp.PnlBHP;
import op.care.dfn.PnlDFN;
import op.care.planung.PnlPlanung;
import op.care.schichtleitung.PnlSchichtleitung;
import op.care.sysfiles.PnlFiles;
import op.care.uebergabe.PnlUebergabe;
import op.care.verordnung.PnlVerordnung;
import op.care.vital.PnlVitalwerte;
import op.threads.HeapStat;
import op.tools.DBRetrieve;
import op.tools.InternalClassACL;
import op.tools.SYSTools;
import op.vorgang.PnlVorgang;
import org.jdesktop.swingx.*;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;

import javax.persistence.Query;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author __USER__
 */
public class FrmPflege extends javax.swing.JFrame {

    public static final String internalClassID = "nursingrecords.main";
    public static final int MAINTAB_UEBERSICHT = 0;
    public static final int UE_UEBERGABE = 0;
    public static final int UE_SCHICHTLEITUNG = 1;
    public static final int MAINTAB_BW = 1;
    public static final int TAB_UEBERSICHT = 0;
    public static final int TAB_PB = 1;
    public static final int TAB_DFN = 2;
    public static final int TAB_BHP = 3;
    public static final int TAB_VITAL = 4;
    public static final int TAB_VERORDNUNG = 5;
    public static final int TAB_INFO = 6;
    public static final int TAB_PPLANUNG = 7;
    public static final int TAB_VORGANG = 8;
    public static final int TAB_FILES = 9;
    public static final int SOZ_BERICHTE = 0;
    public static final int SOZ_NY = 1;
    //private String currentBW = "";
    private Bewohner bewohner = null;
    public String currentTBDatum;
    public String currentTBUhrzeit;
    public String currentTBText;
    public long currentAnamID;
    public HashMap hmC;
    public HashMap fragen;
    public HashMap dfnplanung;

    private boolean initPhase;
    private HeapStat hs;
    public JLabel bwlabel;

    /**
     * Creates new form FrmPflege
     */
    public FrmPflege() {
        initPhase = true;
        initComponents();
        bwlabel = null;
        setTitle(SYSTools.getWindowTitle("Pflegedokumentation"));
        this.setVisible(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        lblServer.setText(OPDE.getUrl());
        lblUser.setText("Benutzer: " + DBRetrieve.getUsername(OPDE.getLogin().getUser().getUKennung()));

        createBewohnerListe();

        hs = new HeapStat(pbHeap, lblHeap);
        hs.start();

        initPhase = false;
        jtpMain.setSelectedIndex(MAINTAB_BW);
        jtpPflegeakte.setSelectedIndex(TAB_UEBERSICHT);

        // Zugriffe
        jtpPflegeakte.setEnabledAt(TAB_PB, OPDE.getInternalClasses().userHasAccessLevelForThisClass(PnlBerichte.internalClassID, InternalClassACL.EXECUTE));
        jtpPflegeakte.setEnabledAt(TAB_FILES, OPDE.getInternalClasses().userHasAccessLevelForThisClass(PnlFiles.internalClassID, InternalClassACL.EXECUTE));
        reloadDisplay();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        pnlStatus = new JPanel();
        lblServer = new JLabel();
        hSpacer1 = new JPanel(null);
        pbHeap = new JProgressBar();
        hSpacer2 = new JPanel(null);
        lblHeap = new JLabel();
        hSpacer3 = new JPanel(null);
        lblUser = new JLabel();
        jtpMain = new JTabbedPane();
        jtpUebersicht = new JTabbedPane();
        pnlUebergabe = new JPanel();
        pnlSchichtleitung = new JPanel();
        pnlPflegeakte = new JPanel();
        pnlBW = new JPanel();
        btnVerlegung = new JButton();
        jspBW = new JScrollPane();
        taskBWContainer = new JXTaskPaneContainer();
        jtpPflegeakte = new JTabbedPane();
        pnlUeber = new JPanel();
        pnlTB = new JPanel();
        pnlDFN = new JPanel();
        pnlBHP = new JPanel();
        pnlVitalDummy = new JPanel();
        pnlVer = new JPanel();
        pnlInfo = new JPanel();
        pnlPPlanung = new JPanel();
        pnlVorgang = new JPanel();
        pnlFiles = new JPanel();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("OpenCare Prototype C");
        Container contentPane = getContentPane();

        //======== pnlStatus ========
        {
            pnlStatus.setBorder(new EtchedBorder());
            pnlStatus.setLayout(new BoxLayout(pnlStatus, BoxLayout.LINE_AXIS));

            //---- lblServer ----
            lblServer.setText("jLabel6");
            pnlStatus.add(lblServer);
            pnlStatus.add(hSpacer1);
            pnlStatus.add(pbHeap);
            pnlStatus.add(hSpacer2);

            //---- lblHeap ----
            lblHeap.setText("jLabel1");
            pnlStatus.add(lblHeap);
            pnlStatus.add(hSpacer3);

            //---- lblUser ----
            lblUser.setText("jLabel6");
            pnlStatus.add(lblUser);
        }

        //======== jtpMain ========
        {
            jtpMain.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    jtpMainStateChanged(e);
                }
            });

            //======== jtpUebersicht ========
            {
                jtpUebersicht.setTabPlacement(SwingConstants.BOTTOM);
                jtpUebersicht.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        jtpUebersichtStateChanged(e);
                    }
                });

                //======== pnlUebergabe ========
                {

                    GroupLayout pnlUebergabeLayout = new GroupLayout(pnlUebergabe);
                    pnlUebergabe.setLayout(pnlUebergabeLayout);
                    pnlUebergabeLayout.setHorizontalGroup(
                        pnlUebergabeLayout.createParallelGroup()
                            .addGap(0, 807, Short.MAX_VALUE)
                    );
                    pnlUebergabeLayout.setVerticalGroup(
                        pnlUebergabeLayout.createParallelGroup()
                            .addGap(0, 483, Short.MAX_VALUE)
                    );
                }
                jtpUebersicht.addTab("\u00dcbergabe", pnlUebergabe);


                //======== pnlSchichtleitung ========
                {

                    GroupLayout pnlSchichtleitungLayout = new GroupLayout(pnlSchichtleitung);
                    pnlSchichtleitung.setLayout(pnlSchichtleitungLayout);
                    pnlSchichtleitungLayout.setHorizontalGroup(
                        pnlSchichtleitungLayout.createParallelGroup()
                            .addGap(0, 807, Short.MAX_VALUE)
                    );
                    pnlSchichtleitungLayout.setVerticalGroup(
                        pnlSchichtleitungLayout.createParallelGroup()
                            .addGap(0, 483, Short.MAX_VALUE)
                    );
                }
                jtpUebersicht.addTab("Schichtleitung", pnlSchichtleitung);

            }
            jtpMain.addTab("\u00dcbersicht", jtpUebersicht);


            //======== pnlPflegeakte ========
            {
                pnlPflegeakte.setLayout(new FormLayout(
                    "pref, $lcgap, default:grow",
                    "default:grow"));

                //======== pnlBW ========
                {
                    pnlBW.setLayout(new FormLayout(
                        "left:default:grow",
                        "fill:default, $rgap, default:grow"));

                    //---- btnVerlegung ----
                    btnVerlegung.setForeground(new Color(255, 51, 0));
                    btnVerlegung.setText("Verlegungsbericht");
                    btnVerlegung.setToolTipText("");
                    btnVerlegung.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            btnVerlegungActionPerformed(e);
                        }
                    });
                    pnlBW.add(btnVerlegung, CC.xy(1, 1, CC.FILL, CC.FILL));

                    //======== jspBW ========
                    {
                        jspBW.addComponentListener(new ComponentAdapter() {
                            @Override
                            public void componentResized(ComponentEvent e) {
                                jspBWComponentResized(e);
                            }
                        });
                        jspBW.setViewportView(taskBWContainer);
                    }
                    pnlBW.add(jspBW, CC.xy(1, 3, CC.FILL, CC.FILL));
                }
                pnlPflegeakte.add(pnlBW, CC.xy(1, 1, CC.FILL, CC.FILL));

                //======== jtpPflegeakte ========
                {
                    jtpPflegeakte.setTabPlacement(SwingConstants.BOTTOM);
                    jtpPflegeakte.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            jtpPflegeakteStateChanged(e);
                        }
                    });

                    //======== pnlUeber ========
                    {

                        GroupLayout pnlUeberLayout = new GroupLayout(pnlUeber);
                        pnlUeber.setLayout(pnlUeberLayout);
                        pnlUeberLayout.setHorizontalGroup(
                            pnlUeberLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlUeberLayout.setVerticalGroup(
                            pnlUeberLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("\u00dcbersicht", pnlUeber);


                    //======== pnlTB ========
                    {

                        GroupLayout pnlTBLayout = new GroupLayout(pnlTB);
                        pnlTB.setLayout(pnlTBLayout);
                        pnlTBLayout.setHorizontalGroup(
                            pnlTBLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlTBLayout.setVerticalGroup(
                            pnlTBLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("Pflegeberichte", pnlTB);


                    //======== pnlDFN ========
                    {

                        GroupLayout pnlDFNLayout = new GroupLayout(pnlDFN);
                        pnlDFN.setLayout(pnlDFNLayout);
                        pnlDFNLayout.setHorizontalGroup(
                            pnlDFNLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlDFNLayout.setVerticalGroup(
                            pnlDFNLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("DFN", pnlDFN);


                    //======== pnlBHP ========
                    {

                        GroupLayout pnlBHPLayout = new GroupLayout(pnlBHP);
                        pnlBHP.setLayout(pnlBHPLayout);
                        pnlBHPLayout.setHorizontalGroup(
                            pnlBHPLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlBHPLayout.setVerticalGroup(
                            pnlBHPLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("BHP", pnlBHP);


                    //======== pnlVitalDummy ========
                    {

                        GroupLayout pnlVitalDummyLayout = new GroupLayout(pnlVitalDummy);
                        pnlVitalDummy.setLayout(pnlVitalDummyLayout);
                        pnlVitalDummyLayout.setHorizontalGroup(
                            pnlVitalDummyLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlVitalDummyLayout.setVerticalGroup(
                            pnlVitalDummyLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("Werte", pnlVitalDummy);


                    //======== pnlVer ========
                    {

                        GroupLayout pnlVerLayout = new GroupLayout(pnlVer);
                        pnlVer.setLayout(pnlVerLayout);
                        pnlVerLayout.setHorizontalGroup(
                            pnlVerLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlVerLayout.setVerticalGroup(
                            pnlVerLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("Verordnungen", pnlVer);


                    //======== pnlInfo ========
                    {

                        GroupLayout pnlInfoLayout = new GroupLayout(pnlInfo);
                        pnlInfo.setLayout(pnlInfoLayout);
                        pnlInfoLayout.setHorizontalGroup(
                            pnlInfoLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlInfoLayout.setVerticalGroup(
                            pnlInfoLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("Informationen", pnlInfo);


                    //======== pnlPPlanung ========
                    {

                        GroupLayout pnlPPlanungLayout = new GroupLayout(pnlPPlanung);
                        pnlPPlanung.setLayout(pnlPPlanungLayout);
                        pnlPPlanungLayout.setHorizontalGroup(
                            pnlPPlanungLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlPPlanungLayout.setVerticalGroup(
                            pnlPPlanungLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("Planung", pnlPPlanung);


                    //======== pnlVorgang ========
                    {

                        GroupLayout pnlVorgangLayout = new GroupLayout(pnlVorgang);
                        pnlVorgang.setLayout(pnlVorgangLayout);
                        pnlVorgangLayout.setHorizontalGroup(
                            pnlVorgangLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlVorgangLayout.setVerticalGroup(
                            pnlVorgangLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("Vorg\u00e4nge", pnlVorgang);


                    //======== pnlFiles ========
                    {

                        GroupLayout pnlFilesLayout = new GroupLayout(pnlFiles);
                        pnlFiles.setLayout(pnlFilesLayout);
                        pnlFilesLayout.setHorizontalGroup(
                            pnlFilesLayout.createParallelGroup()
                                .addGap(0, 643, Short.MAX_VALUE)
                        );
                        pnlFilesLayout.setVerticalGroup(
                            pnlFilesLayout.createParallelGroup()
                                .addGap(0, 483, Short.MAX_VALUE)
                        );
                    }
                    jtpPflegeakte.addTab("Dokumente", pnlFiles);

                }
                pnlPflegeakte.add(jtpPflegeakte, CC.xy(3, 1, CC.DEFAULT, CC.FILL));
            }
            jtpMain.addTab("Pflegeakte", pnlPflegeakte);

        }

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
            contentPaneLayout.createParallelGroup()
                .addComponent(pnlStatus, GroupLayout.DEFAULT_SIZE, 849, Short.MAX_VALUE)
                .addComponent(jtpMain, GroupLayout.DEFAULT_SIZE, 849, Short.MAX_VALUE)
        );
        contentPaneLayout.setVerticalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
                    .addComponent(jtpMain, GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(pnlStatus, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );
        setSize(851, 623);
        setLocationRelativeTo(getOwner());
    }// </editor-fold>//GEN-END:initComponents

    private void jtpMainStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jtpMainStateChanged
        reloadDisplay();
    }//GEN-LAST:event_jtpMainStateChanged

    private void btnVerlegungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerlegungActionPerformed
        if (bewohner == null) return;
        print(op.care.DBHandling.getUeberleitung(bewohner, true, true, true, false, false, true, true, true, true, false));
    }//GEN-LAST:event_btnVerlegungActionPerformed

    private void jspBWComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jspBWComponentResized
        //formatBWTable();
    }//GEN-LAST:event_jspBWComponentResized

    private void jtpPflegeakteStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jtpPflegeakteStateChanged
        reloadDisplay();
    }//GEN-LAST:event_jtpPflegeakteStateChanged

    private void jtpUebersichtStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jtpUebersichtStateChanged
        reloadDisplay();
    }//GEN-LAST:event_jtpUebersichtStateChanged

    private void print(String html) {
        try {
            // Create temp file.
            File temp = File.createTempFile("ueberleitung", ".html");

            // Delete temp file when program exits.
            temp.deleteOnExit();

            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write(SYSTools.htmlUmlautConversion(html));

            out.close();
            //DlgFilesAssign.handleFile(this, temp.getAbsolutePath(), Desktop.Action.OPEN);
        } catch (IOException e) {
        }
    }

    private void createBewohnerListe() {
        Query query = OPDE.getEM().createNamedQuery("Bewohner.findAllActiveSortedByStationen");
        Iterator<Bewohner> it = query.getResultList().iterator();

        Stationen station = null;
        Stationen meineStation = StationenTools.getStation4ThisHost();
        JXTaskPane currentStationsPane = null;

        while (it.hasNext()) {
            final Bewohner innerbewohner = it.next();
            if (station != innerbewohner.getStation()) {
                station = innerbewohner.getStation();
                currentStationsPane = new JXTaskPane(station.getBezeichnung());
                currentStationsPane.setSpecial(station.equals(meineStation));
                currentStationsPane.setCollapsed(!currentStationsPane.isSpecial());
                taskBWContainer.add((JPanel) currentStationsPane);
            }

            currentStationsPane.add(new AbstractAction() {
                {
                    String titel = innerbewohner.getNachname() + ", " + innerbewohner.getVorname() + " [" + innerbewohner.getBWKennung() + "]";
                    putValue(Action.NAME, "<html><font color=\""+ (BewohnerTools.isWeiblich(innerbewohner) ? "red" : "blue") +"\">"+titel+"</font></html>");
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    bewohner = innerbewohner;
                    reloadDisplay();
                }
            });
        }
    }

    public void dispose() {
        hs.interrupt();
        cleanup();
        super.dispose();
    }

//    private void formatBWTable() {
//        JViewport jv = (JViewport) tblBW.getParent();
//        JScrollPane jsp = (JScrollPane) jv.getParent();
//        Dimension dim = jsp.getSize();
//        // Größe der Massnahmen Spalten ändern.
//        int width = dim.width;
//        TableColumnModel tcm1 = tblBW.getColumnModel();
//
////        // Zu Beginn der Applikation steht noch ein standardmodell drin.
////        // das hat nur 4 Spalten. solange braucht sich dieser handler nicht
////        // damit zu befassen.
////        if (tcm1.getColumnCount() != ) {
////            return;
////        }
//
//        tcm1.getColumn(0).setPreferredWidth(width);
//        //tcm1.getColumn(1).setPreferredWidth(70);
//
//        tcm1.getColumn(0).setHeaderValue("Name");
//        //tcm1.getColumn(1).setHeaderValue("Info");
//
//        tcm1.getColumn(0).setCellRenderer(new RNDBW());
//        //tcm1.getColumn(1).setCellRenderer(new RNDBW());
//    }

    public void cleanup() {
        // Aufräumen
        for (int i = 0; i < jtpPflegeakte.getTabCount(); i++) {
            if (jtpPflegeakte.getComponentAt(i) != null && jtpPflegeakte.getComponentAt(i) instanceof CleanablePanel) {
                CleanablePanel cp = (CleanablePanel) jtpPflegeakte.getComponentAt(i);
                cp.cleanup();
                SYSTools.unregisterListeners((JComponent) jtpPflegeakte.getComponentAt(i));
                jtpPflegeakte.setComponentAt(i, null);
            }
        }
        if (jtpMain.getComponentAt(MAINTAB_UEBERSICHT) != null && jtpMain.getComponentAt(MAINTAB_UEBERSICHT) instanceof CleanablePanel) {
            CleanablePanel cp = (CleanablePanel) jtpMain.getComponentAt(MAINTAB_UEBERSICHT);
            cp.cleanup();
            jtpMain.setComponentAt(MAINTAB_UEBERSICHT, null);
        }
    }

    private void reloadDisplay() {
        if (initPhase) {
            return;
        }
        cleanup();
        switch (jtpMain.getSelectedIndex()) {
            case MAINTAB_UEBERSICHT: {
                switch (jtpUebersicht.getSelectedIndex()) {
                    case UE_UEBERGABE: {
                        jtpUebersicht.setComponentAt(UE_UEBERGABE, new PnlUebergabe(this));
                        jtpUebersicht.setTitleAt(UE_UEBERGABE, "Übergabe");
                        break;
                    }
                    case UE_SCHICHTLEITUNG: {
                        jtpUebersicht.setComponentAt(UE_SCHICHTLEITUNG, new PnlSchichtleitung(this));
                        jtpUebersicht.setTitleAt(UE_SCHICHTLEITUNG, "Schichtleitung");
                        break;
                    }
                    default: {
                    }
                }

                break;
            }
            case MAINTAB_BW: {
                if (bewohner != null) {
                    switch (jtpPflegeakte.getSelectedIndex()) {
                        case TAB_UEBERSICHT: {
                            jtpPflegeakte.setComponentAt(TAB_UEBERSICHT, new PnlBWUebersicht(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_UEBERSICHT, "Übersicht");
                            break;
                        }
                        case TAB_PB: {
                            jtpPflegeakte.setComponentAt(TAB_PB, new PnlBerichte(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_PB, "Pflegeberichte");
                            break;
                        }
                        case TAB_DFN: {
                            jtpPflegeakte.setComponentAt(TAB_DFN, new PnlDFN(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_DFN, "DFN");
                            break;
                        }
                        case TAB_VITAL: {
                            jtpPflegeakte.setComponentAt(TAB_VITAL, new PnlVitalwerte(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_VITAL, "Werte");
                            break;
                        }
                        case TAB_INFO: {
                            jtpPflegeakte.setComponentAt(TAB_INFO, new op.care.bwinfo.PnlInfo(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_INFO, "Informationen");
                            break;
                        }
                        case TAB_BHP: {
                            jtpPflegeakte.setComponentAt(TAB_BHP, new PnlBHP(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_BHP, "BHP");
                            break;
                        }
                        case TAB_PPLANUNG: {
                            jtpPflegeakte.setComponentAt(TAB_PPLANUNG, new PnlPlanung(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_PPLANUNG, "Planung");
                            break;
                        }
                        case TAB_VERORDNUNG: {
                            jtpPflegeakte.setComponentAt(TAB_VERORDNUNG, new PnlVerordnung(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_VERORDNUNG, "Verordnungen");
                            break;
                        }
                        case TAB_VORGANG: {
                            final PnlVorgang pnlVorgang = new PnlVorgang(this, bewohner);
                            CleanablePanel cp = new CleanablePanel() {
                                @Override
                                public void cleanup() {
                                    pnlVorgang.cleanup();
                                }
                            };
                            cp.setLayout(new VerticalLayout(10));
                            cp.add(BewohnerTools.getBWLabel(bewohner));
                            cp.add(pnlVorgang);

                            jtpPflegeakte.setComponentAt(TAB_VORGANG, cp);
                            jtpPflegeakte.setTitleAt(TAB_VORGANG, "Vorgänge");
                            break;
                        }
                        case TAB_FILES: {
                            jtpPflegeakte.setComponentAt(TAB_FILES, new PnlFiles(this, bewohner));
                            jtpPflegeakte.setTitleAt(TAB_FILES, "Dokumente");
                            break;
                        }
                        default: {
                        }
                    }
                    break;
                } else {
                    // Kein Bewohner gewählt
                    jtpPflegeakte.setComponentAt(TAB_UEBERSICHT, new JPanel());
                    jtpPflegeakte.setTitleAt(TAB_UEBERSICHT, "Übersicht");
                }
            }

            default: {
            }
        }
    }

//    private void reloadTable(Bewohner bewohner) {
//        // Bewohner-Liste
//
//        if (cbArchiv.isSelected()) { // Archivmodus gewünscht.
//            tblBW.setModel(new TMBW());
//            initPhase = true;
//            //cmbStation.setSelectedItem("Alle");
//            cmbStation.setEnabled(false);
//            initPhase = false;
//        } else {
//            tblBW.setModel(new TMBW((Stationen) cmbStation.getSelectedItem()));
//        }
//
//        tblBW.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
//        lsmbw.addListSelectionListener(lslBW);
//        if (tblBW.getModel().getRowCount() > 0) {
//            tblBW.setRowSelectionInterval(0, 0);
//        }
//        TableColumnModel tcm1 = tblBW.getColumnModel();
////        tcm1.getColumn(0).setHeaderValue("Name");
////        tcm1.getColumn(1).setHeaderValue("Info");
//        jspBW.dispatchEvent(new ComponentEvent(jspBW, ComponentEvent.COMPONENT_RESIZED));
//    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel pnlStatus;
    private JLabel lblServer;
    private JPanel hSpacer1;
    private JProgressBar pbHeap;
    private JPanel hSpacer2;
    private JLabel lblHeap;
    private JPanel hSpacer3;
    private JLabel lblUser;
    private JTabbedPane jtpMain;
    private JTabbedPane jtpUebersicht;
    private JPanel pnlUebergabe;
    private JPanel pnlSchichtleitung;
    private JPanel pnlPflegeakte;
    private JPanel pnlBW;
    private JButton btnVerlegung;
    private JScrollPane jspBW;
    private JXTaskPaneContainer taskBWContainer;
    private JTabbedPane jtpPflegeakte;
    private JPanel pnlUeber;
    private JPanel pnlTB;
    private JPanel pnlDFN;
    private JPanel pnlBHP;
    private JPanel pnlVitalDummy;
    private JPanel pnlVer;
    private JPanel pnlInfo;
    private JPanel pnlPPlanung;
    private JPanel pnlVorgang;
    private JPanel pnlFiles;
    // End of variables declaration//GEN-END:variables


    public javax.swing.JTabbedPane getjtpPflegeakte() {
        return jtpPflegeakte;
    }

}
