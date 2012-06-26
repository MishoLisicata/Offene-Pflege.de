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
package op.bw.tg;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.jidesoft.pane.CollapsiblePane;
import com.jidesoft.pane.CollapsiblePanes;
import com.jidesoft.popup.JidePopup;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.swing.JideButton;
import entity.*;
import op.OPDE;
import op.threads.DisplayMessage;
import op.tools.*;
import org.apache.commons.collections.Closure;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.JXSearchField;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jdesktop.swingx.VerticalLayout;
import tablemodels.TMBarbetrag;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
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
import java.text.*;
import java.util.*;
import java.util.List;

/**
 * @author tloehr
 */
public class PnlTG extends CleanablePanel {
    public static final String internalClassID = "admin.residents.cash";
    private TableModelListener tml;

    private Date min;
    private Date max;
    private BigDecimal betrag;
    private JPopupMenu menu;
    private Bewohner bewohner;
    private CollapsiblePane panelText, panelTime;
    private JScrollPane jspSearch;
    private CollapsiblePanes searchPanes;
    private JComboBox cmbVon, cmbBis, cmbMonat;
    private JXSearchField txtBW;
    private boolean ignoreDateComboEvent;
    private HashMap<Bewohner, Object[]> searchSaldoButtonMap;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    private JComboBox cmbPast;
    private Closure bwchange;

    /**
     * Creates new form FrmBWAttr
     */
    private void tblTGMousePressed(MouseEvent e) {

        Point p = e.getPoint();
        Point p2 = e.getPoint();
        // Convert a coordinate relative to a component's bounds to screen coordinates
        SwingUtilities.convertPointToScreen(p2, tblTG);

        final Point screenposition = p2;

        final int row = tblTG.rowAtPoint(p);
        final int col = tblTG.columnAtPoint(p);
        OPDE.debug("COLUMN: " + col);
        final ListSelectionModel lsm = tblTG.getSelectionModel();
        boolean singleRowSelected = lsm.getMaxSelectionIndex() == lsm.getMinSelectionIndex();


        if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {
            lsm.setSelectionInterval(row, row);
        }

        // Kontext Menü
        if (singleRowSelected && e.isPopupTrigger()) {

            SYSTools.unregisterListeners(menu);
            menu = new JPopupMenu();

            final JMenuItem itemPopupEdit = new JMenuItem("Eintrag bearbeiten");
            itemPopupEdit.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    final TMBarbetrag tm = (TMBarbetrag) tblTG.getModel();
                    final Barbetrag mytg = tm.getListData().get(tm.getModelRow(row));  // Rechnet die Zeile um. Berücksichtigt die Zusammenfassungszeile

                    final JidePopup popup = new JidePopup();
//
                    popup.setMovable(false);
                    popup.getContentPane().setLayout(new BoxLayout(popup.getContentPane(), BoxLayout.LINE_AXIS));

                    final JTextField txtEditor = new JTextField(20);

                    switch (col) {
                        case TMBarbetrag.COL_Datum: {
                            txtEditor.setText(DateFormat.getDateInstance().format(mytg.getBelegDatum()));
                            break;
                        }
                        case TMBarbetrag.COL_Text: {
                            txtEditor.setText(mytg.getBelegtext().trim());
                            break;
                        }
                        case TMBarbetrag.COL_Betrag: {
                            NumberFormat nf = DecimalFormat.getCurrencyInstance();
                            txtEditor.setText(nf.format(mytg.getBetrag()));
                            break;
                        }
                        default: {

                        }
                    }

                    popup.getContentPane().add(txtEditor);

                    final JButton saveButton = new JButton(new ImageIcon(getClass().getResource("/artwork/22x22/bw/apply.png")));
                    saveButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            EntityManager em = OPDE.createEM();
                            try {
                                em.getTransaction().begin();
                                Barbetrag mytg2 = em.merge(mytg);
                                switch (col) {
                                    case TMBarbetrag.COL_Datum: {
                                        mytg2.setBelegDatum(checkDatum(txtEditor.getText(), mytg2.getBelegDatum()));
                                        break;
                                    }
                                    case TMBarbetrag.COL_Text: {
                                        mytg2.setBelegtext(txtEditor.getText());
                                        break;
                                    }
                                    case TMBarbetrag.COL_Betrag: {
                                        mytg2.setBetrag(checkBetrag(txtEditor.getText(), mytg2.getBetrag()));
                                        break;
                                    }
                                    default: {

                                    }
                                }
                                em.getTransaction().commit();
                                tm.getListData().set(tm.getListData().indexOf(mytg), mytg2);
                                tm.fireTableCellUpdated(row, col);
                                popup.hidePopup();

                                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Datensatz '" + mytg.getBelegtext() + "' geändert.", 2));

                                if (col == TMBarbetrag.COL_Datum && !panelTime.isCollapsed()) {
                                    if (min.after(mytg2.getBelegDatum())) {
                                        // Neuer Eintrag liegt ausserhalb des bisherigen Intervals.
                                        min = SYSCalendar.bom(mytg2.getBelegDatum());
                                        initSearchTime();
                                    }
                                    cmbMonat.setSelectedItem(SYSCalendar.bom(mytg2.getBelegDatum()));
                                } else if (col == TMBarbetrag.COL_Betrag) {
                                    summeNeuRechnen();
                                    BigDecimal saldo = (BigDecimal) searchSaldoButtonMap.get(bewohner)[0];
                                    JideButton button = (JideButton) searchSaldoButtonMap.get(bewohner)[1];

                                    saldo = saldo.add(mytg2.getBetrag());
                                    searchSaldoButtonMap.put(bewohner, new Object[]{button, saldo});

                                    String titel = "<html>" + bewohner.getNachname() + ", " + bewohner.getVorname() + " [" + bewohner.getBWKennung() + "] <b><font " + (saldo.compareTo(BigDecimal.ZERO) < 0 ? "color=\"red\"" : "color=\"black\"") + ">" + currencyFormat.format(saldo) + "</font></b></html>";
                                    button.setText(titel);
                                }

                            } catch (Exception e) {
                                em.getTransaction().rollback();
                            } finally {
                                em.close();
                            }
                        }
                    });
                    txtEditor.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            saveButton.doClick();
                        }
                    });

//                    popup.setOwner(tblTG);
                    popup.getContentPane().add(new JPanel().add(saveButton));
                    popup.setDefaultFocusComponent(txtEditor);
                    popup.showPopup(screenposition.x, screenposition.y);
                }
            });
            menu.add(itemPopupEdit);
            itemPopupEdit.setEnabled(col != TMBarbetrag.COL_Zeilensaldo && OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.UPDATE) && ((TMBarbetrag) tblTG.getModel()).isReal(row));
            //
            // =====
            //
            JMenuItem itemPopupDelete = new JMenuItem("Eintrag löschen");
            itemPopupDelete.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    final TMBarbetrag tm = (TMBarbetrag) tblTG.getModel();
                    final Barbetrag mytg = tm.getListData().get(tm.getModelRow(row));  // Rechnet die Zeile um. Berücksichtigt die Zusammenfassungszeile

                    final JOptionPane loeschenPane = new JOptionPane("Sie löschen nun den Datensatz '" + mytg.getBelegtext() + "'.\nMöchten Sie das ?", JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
                    loeschenPane.addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                            if (propertyChangeEvent.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {

                                if (((Integer) propertyChangeEvent.getNewValue()) == JOptionPane.YES_OPTION) {
                                    EntityTools.delete(mytg);
                                    tm.getListData().remove(mytg);
                                    tm.fireTableRowsDeleted(row, row);
                                    summeNeuRechnen();
                                    OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Datensatz '" + mytg.getBelegtext() + "' gelöscht.", 2));
                                }
//                                loeschenPane.setLocation(OPDE.getMainframe().getLocationForDialog(loeschenPane.getSize()));
                                loeschenPane.setVisible(false);
                            }
                        }
                    });
                    loeschenPane.setLocation(OPDE.getMainframe().getLocationForDialog(loeschenPane.getSize()));
                    loeschenPane.setVisible(true);
                }
            });
            menu.add(itemPopupDelete);
            itemPopupDelete.setEnabled(OPDE.getAppInfo().userHasAccessLevelForThisClass(internalClassID, InternalClassACL.DELETE) && ((TMBarbetrag) tblTG.getModel()).isReal(row));
            menu.show(e.getComponent(), (int) p.getX(), (int) p.getY());
        }
    }

    public PnlTG(JScrollPane jspSearch, Closure bwchange) {
        this.jspSearch = jspSearch;
        bewohner = null;
        this.bwchange = bwchange;
        initComponents();
        setVisible(true);
        tblTG.setModel(new DefaultTableModel());
        ignoreDateComboEvent = true;
        prepareSearchArea();
        setMinMax();
        initSearchTime();

        ignoreDateComboEvent = false;

    }

     @Override
    public void reload() {
         reloadTable();
    }

    @Override
    public void cleanup() {
        searchPanes.removeAll();
        SYSTools.unregisterListeners(cmbPast);
        SYSTools.unregisterListeners(cmbVon);
        SYSTools.unregisterListeners(cmbBis);
        OPDE.getDisplayManager().clearSubMessages();
        jspData.setViewportView(null);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        pnlBarbetrag = new JPanel();
        jPanel4 = new JPanel();
        jspData = new JScrollPane();
        tblTG = new JTable();
        jPanel5 = new JPanel();
        txtDatum = new JTextField();
        txtBelegtext = new JTextField();
        txtBetrag = new JTextField();

        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //======== pnlBarbetrag ========
        {
            pnlBarbetrag.setLayout(new FormLayout(
                "default:grow, $lcgap, pref",
                "fill:default:grow, $lgap, fill:default, $lgap, $rgap"));

            //======== jPanel4 ========
            {
                jPanel4.setBorder(new SoftBevelBorder(SoftBevelBorder.RAISED));
                jPanel4.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));

                //======== jspData ========
                {
                    jspData.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentResized(ComponentEvent e) {
                            jspDataComponentResized(e);
                        }
                    });

                    //---- tblTG ----
                    tblTG.setModel(new DefaultTableModel(
                        new Object[][] {
                            {null, null, null, null},
                            {null, null, null, null},
                            {null, null, null, null},
                            {null, null, null, null},
                        },
                        new String[] {
                            "Title 1", "Title 2", "Title 3", "Title 4"
                        }
                    ));
                    tblTG.setFont(new Font("sansserif", Font.PLAIN, 14));
                    tblTG.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            tblTGMousePressed(e);
                        }
                    });
                    jspData.setViewportView(tblTG);
                }
                jPanel4.add(jspData, CC.xy(1, 1, CC.DEFAULT, CC.FILL));
            }
            pnlBarbetrag.add(jPanel4, CC.xywh(1, 1, 3, 1));

            //======== jPanel5 ========
            {
                jPanel5.setBorder(LineBorder.createBlackLineBorder());
                jPanel5.setLayout(new FormLayout(
                    "default:grow(0.30000000000000004), $lcgap, default:grow(0.7000000000000001), $lcgap, 30dlu:grow(0.30000000000000004)",
                    "fill:default"));

                //---- txtDatum ----
                txtDatum.setEnabled(false);
                txtDatum.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        txtDatumActionPerformed(e);
                    }
                });
                txtDatum.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        txtDatumFocusGained(e);
                    }
                    @Override
                    public void focusLost(FocusEvent e) {
                        txtDatumFocusLost(e);
                    }
                });
                jPanel5.add(txtDatum, CC.xy(1, 1, CC.FILL, CC.DEFAULT));

                //---- txtBelegtext ----
                txtBelegtext.setEnabled(false);
                txtBelegtext.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        txtBelegtextActionPerformed(e);
                    }
                });
                txtBelegtext.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        txtBelegtextFocusGained(e);
                    }
                    @Override
                    public void focusLost(FocusEvent e) {
                        txtBelegtextFocusLost(e);
                    }
                });
                jPanel5.add(txtBelegtext, CC.xy(3, 1, CC.FILL, CC.DEFAULT));

                //---- txtBetrag ----
                txtBetrag.setHorizontalAlignment(SwingConstants.RIGHT);
                txtBetrag.setEnabled(false);
                txtBetrag.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        txtBetragActionPerformed(e);
                    }
                });
                txtBetrag.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        txtBetragFocusGained(e);
                    }
                    @Override
                    public void focusLost(FocusEvent e) {
                        txtBetragFocusLost(e);
                    }
                });
                jPanel5.add(txtBetrag, CC.xy(5, 1, CC.FILL, CC.DEFAULT));
            }
            pnlBarbetrag.add(jPanel5, CC.xywh(1, 3, 3, 1));
        }
        add(pnlBarbetrag);
    }// </editor-fold>//GEN-END:initComponents

//    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
//        if (!btnEdit.isSelected()) {
//            if (tblTG.getCellEditor() != null) {
//                tblTG.getCellEditor().cancelCellEditing();
//            }
//        }
//        ((TMBarbetrag) tblTG.getModel()).setEditable(btnEdit.isSelected());
//    }//GEN-LAST:event_btnEditActionPerformed

    private void txtBelegtextFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtBelegtextFocusLost
        if (txtBelegtext.getText().trim().isEmpty()) {
            txtBelegtext.setText("Geben Sie einen Belegtext ein.");
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Sie können das Belegtextfeld nicht leer lassen.", 2));
//            lblMessage.setText(timeDF.format(new Date()) + " Uhr : " + "Sie können das Belegtextfeld nicht leer lassen.");
        }
    }//GEN-LAST:event_txtBelegtextFocusLost

    private void txtBelegtextFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtBelegtextFocusGained
        ((JTextField) evt.getSource()).selectAll();
    }//GEN-LAST:event_txtBelegtextFocusGained

    private void txtBetragFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtBetragFocusGained
        ((JTextField) evt.getSource()).selectAll();
    }//GEN-LAST:event_txtBetragFocusGained

    private void txtBetragFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtBetragFocusLost
        betrag = SYSTools.parseCurrency(txtBetrag.getText());
        if (betrag != null) {
            if (!betrag.equals(BigDecimal.ZERO)) {
                insert();
                summeNeuRechnen();
            } else {
                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Beträge mit '0,00 " + SYSConst.eurosymbol + "' werden nicht angenommen.", 2));
//                lblMessage.setText(timeDF.format(new Date()) + " Uhr : " + "Beträge mit '0,00 " + SYSConst.eurosymbol + "' werden nicht angenommen.");
            }

        } else {
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Bitte geben Sie Euro Beträge in der folgenden Form ein: '10,0 " + SYSConst.eurosymbol + "'", 2));
//            lblMessage.setText(timeDF.format(new Date()) + " Uhr : " + "Bitte geben Sie Euro Beträge in der folgenden Form ein: '10,0 " + SYSConst.eurosymbol + "'");
            betrag = BigDecimal.ZERO;
        }
        txtBetrag.setText(NumberFormat.getCurrencyInstance().format(betrag));
    }//GEN-LAST:event_txtBetragFocusLost

    private void txtBetragActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBetragActionPerformed
        txtDatum.requestFocus();
    }//GEN-LAST:event_txtBetragActionPerformed

    private void txtBelegtextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBelegtextActionPerformed
        txtBetrag.requestFocus();
    }//GEN-LAST:event_txtBelegtextActionPerformed

    private void txtDatumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDatumActionPerformed
        txtBelegtext.requestFocus();
    }//GEN-LAST:event_txtDatumActionPerformed

    private void txtDatumFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtDatumFocusLost
        GregorianCalendar gc;
        try {
            gc = SYSCalendar.erkenneDatum(((JTextField) evt.getSource()).getText());
        } catch (NumberFormatException ex) {
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Sie haben ein falsches Datum eingegeben. Wurde auf heute zurückgesetzt.", 2));
//            lblMessage.setText(timeDF.format(new Date()) + " Uhr : Sie haben ein falsches Datum eingegeben. Wurde auf heute zurückgesetzt.");
            gc = SYSCalendar.today();
        }
        // Datum in der Zukunft ?
        if (SYSCalendar.sameDay(gc, SYSCalendar.today()) > 0) {
            gc = SYSCalendar.today();
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Sie haben ein Datum in der Zukunft eingegeben. Wurde auf heute zurückgesetzt.", 2));
//            lblMessage.setText(timeDF.format(new Date()) + " Uhr : Sie haben ein Datum in der Zukunft eingegeben. Wurde auf heute zurückgesetzt.");
        }
        ((JTextField) evt.getSource()).setText(SYSCalendar.printGCGermanStyle(gc));
    }//GEN-LAST:event_txtDatumFocusLost

    private void txtDatumFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtDatumFocusGained
        ((JTextField) evt.getSource()).selectAll();
    }//GEN-LAST:event_txtDatumFocusGained

    private void btnPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintActionPerformed
        TMBarbetrag tm = (TMBarbetrag) tblTG.getModel();
        printSingle(tm.getListData(), tm.getVortrag());
    }//GEN-LAST:event_btnPrintActionPerformed

    private void printSingle(List<Barbetrag> liste, BigDecimal vortrag) {

        try {
            // Create temp file.
            File temp = File.createTempFile("barbetrag", ".html");

            // Delete temp file when program exits.
            temp.deleteOnExit();

            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            String html = SYSTools.htmlUmlautConversion(BarbetragTools.getEinzelnAsHTML(liste, vortrag, bewohner));

            out.write(html);

            out.close();
            SYSPrint.handleFile(temp.getAbsolutePath(), Desktop.Action.OPEN);
        } catch (IOException e) {
            new DlgException(e);
        }

    }

    private void jspDataComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jspDataComponentResized
        JScrollPane jsp = (JScrollPane) evt.getComponent();
        Dimension dim = jsp.getSize();
        // Größe der Text Spalten im DFN ändern.
        // Summe der fixen Spalten  = 175 + ein bisschen
        int textWidth = dim.width - 370;

        if (tblTG.getModel().getRowCount() == 0) {
            return;
        }
        TableColumnModel tcm1 = tblTG.getColumnModel();


//        SYSTools.packTable(tblTG, 5);

        tcm1.getColumn(0).setHeaderValue("Belegdatum");
        tcm1.getColumn(0).setPreferredWidth(100);
        tcm1.getColumn(1).setHeaderValue("Belegtext");
        tcm1.getColumn(1).setPreferredWidth(textWidth);
        tcm1.getColumn(2).setHeaderValue("Betrag");
        tcm1.getColumn(2).setPreferredWidth(120);
        tcm1.getColumn(3).setHeaderValue("Zeilensaldo");
        tcm1.getColumn(3).setPreferredWidth(120);


    }//GEN-LAST:event_jspDataComponentResized


    /**
     * Setzt den Zeitraum, innerhalb dessen die Belege in der Tabelle angezeigt werden können. Nicht unbedingt werden.
     */
    private void setMinMax() {
        // Ermittelt die maximale Ausdehnung (chronologisch gesehen) aller Belege für einen bestimmten BW

        min = SYSCalendar.today_date();

        if (bewohner != null) {
            EntityManager em = OPDE.createEM();
            Query query = em.createQuery("SELECT MIN(tg.belegDatum) FROM Barbetrag tg WHERE tg.bewohner = :bewohner");
            query.setParameter("bewohner", bewohner);
            min = (Date) query.getSingleResult();
            em.close();
        }

        min = SYSCalendar.bom(min == null ? SYSCalendar.today_date() : min);
        max = SYSCalendar.eom(SYSCalendar.today_date());

    }

    private void summeNeuRechnen() {
        TMBarbetrag tm = (TMBarbetrag) tblTG.getModel();
        BigDecimal zeilensaldo = tm.getZeilenSaldo();

//        BigDecimal summe = (BigDecimal) DBHandling.getSingleValue("Taschengeld", "SUM(Betrag)", "BWKennung", currentBW);

        NumberFormat nf = NumberFormat.getCurrencyInstance();
//        lblBetrag.setText(nf.format(zeilensaldo));
//        if (zeilensaldo.compareTo(BigDecimal.ZERO) < 0) {
//            lblBetrag.setForeground(Color.RED);
//        } else {
//            lblBetrag.setForeground(Color.BLACK);
//        }

        OPDE.getDisplayManager().setMainMessage(BewohnerTools.getBWLabelText(bewohner) + ", Saldo: " + nf.format(zeilensaldo));


    }

    private void reloadDisplay() {
//        lblMessage.setText("");

        // Welcher Tab ist gerade ausgewählt ?

        setMinMax();
        initSearchTime();
        bwchange.execute(bewohner);

        if (bewohner != null) {
//                    BewohnerTools.setBWLabel(lblBW, bewohner);

            txtDatum.setText(SYSCalendar.printGermanStyle(SYSCalendar.today_date()));
            txtBelegtext.setText("Bitte geben Sie einen Belegtext ein.");
            txtBetrag.setText("0,00 " + SYSConst.eurosymbol);
            betrag = BigDecimal.ZERO;
            txtDatum.setEnabled(true);
            txtBelegtext.setEnabled(true);
            txtBetrag.setEnabled(true);
            reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
            summeNeuRechnen();
        }


    }

    private void updateSummenAngabe() {
        EntityManager em = OPDE.createEM();
        Query query = em.createQuery("SELECT SUM(tg.betrag) FROM Barbetrag tg ");
        BigDecimal summe = BigDecimal.ZERO;
        try {
            summe = (BigDecimal) query.getSingleResult();
        } catch (NoResultException nre) {
            summe = BigDecimal.ZERO;
        } catch (Exception e) {
            OPDE.fatal(e);
        }

        NumberFormat nf = NumberFormat.getCurrencyInstance();
        String summentext = nf.format(summe);

        // Ist auch eine Anzeige für die Vergangenheit gewünscht ?
        // Nur wenn ein anderer Monat als der aktuelle gewählt ist.
        if (cmbPast.getSelectedIndex() < cmbPast.getModel().getSize() - 1) {
            Query queryPast = em.createQuery("SELECT SUM(tg.betrag) FROM Barbetrag tg WHERE tg.belegDatum <= :datum");
            queryPast.setParameter("datum", SYSCalendar.eom((Date) cmbPast.getSelectedItem()));

            BigDecimal summePast = BigDecimal.ZERO;
            try {
                summePast = (BigDecimal) queryPast.getSingleResult();
            } catch (NoResultException nre) {
                summePast = BigDecimal.ZERO;
            } catch (Exception e) {
                OPDE.fatal(e);
            }

            summentext += " (" + nf.format(summePast) + ")";
        }

        em.close();

        OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Gesamtsaldo aller Barbeträge: " + summentext, 10));

//        if (summe.compareTo(BigDecimal.ZERO) < 0) {
//            lblSumme.setForeground(Color.RED);
//        } else {
//            lblSumme.setForeground(Color.BLACK);
//        }

    }

    private void insert() {

        Date datum = new Date(SYSCalendar.erkenneDatum(txtDatum.getText()).getTimeInMillis());
        TMBarbetrag tm = (TMBarbetrag) tblTG.getModel();

        Barbetrag barbetrag = new Barbetrag(datum, txtBelegtext.getText().trim(), betrag, bewohner, OPDE.getLogin().getUser());
        EntityTools.persist(barbetrag);
        tm.getListData().add(barbetrag);
        Collections.sort(tm.getListData());

//        schaltet auf den Monat um, in dem der letzte Beleg eingegeben wurde.
//        Sofern die ein bestimmter Monat eingestellt war.
        if (!panelTime.isCollapsed()) {
//            GregorianCalendar gcDatum = SYSCalendar.toGC(datum);
            if (min.after(datum)) {
                // Neuer Eintrag liegt ausserhalb des bisherigen Intervals.
                min = SYSCalendar.bom(datum);
                initSearchTime();
            }
            cmbMonat.setSelectedItem(SYSCalendar.bom(datum));
        } else {
            reloadTable();
        }

//        reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
        txtBelegtext.setText("Bitte geben Sie einen Belegtext ein.");
        txtBetrag.setText("0.00 " + SYSConst.eurosymbol);
        betrag = BigDecimal.ZERO;
        txtDatum.requestFocus();

        BigDecimal saldo = (BigDecimal) searchSaldoButtonMap.get(bewohner)[0];
        JideButton button = (JideButton) searchSaldoButtonMap.get(bewohner)[1];

        saldo = saldo.add(barbetrag.getBetrag());
        searchSaldoButtonMap.put(bewohner, new Object[]{saldo, button});

        String titel = "<html>" + bewohner.getNachname() + ", " + bewohner.getVorname() + " [" + bewohner.getBWKennung() + "] <b><font " + (saldo.compareTo(BigDecimal.ZERO) < 0 ? "color=\"red\"" : "color=\"black\"") + ">" + currencyFormat.format(saldo) + "</font></b></html>";
        button.setText(titel);


        // Das hier markiert den zuletzt eingefügten Datensatz.
        int index = tm.getListData().indexOf(barbetrag);
        ListSelectionModel lsm = tblTG.getSelectionModel();
        lsm.setSelectionInterval(index, index);
        // Das hier rollt auf den zuletzt eingefügten Datensatz.
        tblTG.invalidate();
        Rectangle rect = tblTG.getCellRect(index, 0, true);
        tblTG.scrollRectToVisible(rect);
    }

    private void reloadTable() {
        reloadTable(null, null);
    }

    private void reloadTable(Date von, Date bis) {
        if (bewohner == null) {
            return;
        }

        tml = new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() == 2) { // Betrag hat sich geändert
                    summeNeuRechnen();
                }
            }
        };

        tblTG.setModel(new TMBarbetrag(bewohner, von, bis, false));
        tblTG.getModel().addTableModelListener(tml);
        tblTG.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblTG.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        jspData.dispatchEvent(new ComponentEvent(jspData, ComponentEvent.COMPONENT_RESIZED));

        tblTG.getColumnModel().getColumn(2).setCellRenderer(new CurrencyRenderer());
        tblTG.getColumnModel().getColumn(3).setCellRenderer(new CurrencyRenderer());

//        tblTG.getColumnModel().getColumn(0).setCellEditor(new CEDefault());
//        tblTG.getColumnModel().getColumn(1).setCellEditor(new CEDefault());
//        tblTG.getColumnModel().getColumn(2).setCellEditor(new CEDefault());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel pnlBarbetrag;
    private JPanel jPanel4;
    private JScrollPane jspData;
    private JTable tblTG;
    private JPanel jPanel5;
    private JTextField txtDatum;
    private JTextField txtBelegtext;
    private JTextField txtBetrag;
    // End of variables declaration//GEN-END:variables


    private void prepareSearchArea() {
        searchPanes = new CollapsiblePanes();
        searchPanes.setLayout(new JideBoxLayout(searchPanes, JideBoxLayout.Y_AXIS));
        jspSearch.setViewportView(searchPanes);

        JPanel mypanel = new JPanel();
        mypanel.setLayout(new VerticalLayout());
        mypanel.setBackground(Color.WHITE);
//        mypanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JideButton printButton = GUITools.createHyperlinkButton(OPDE.lang.getString("misc.commands.print"), new ImageIcon(getClass().getResource("/artwork/22x22/bw/printer.png")), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (bewohner != null) {
                    TMBarbetrag tm = (TMBarbetrag) tblTG.getModel();
                    printSingle(tm.getListData(), tm.getVortrag());
                } else {
                    OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Wählen Sie zuerst eine(n) BewohnerIn aus.", 2));
                }
            }
        });
        mypanel.add(printButton);

        if (OPDE.isAdmin()) {
            JideButton gesamtSummeButton = GUITools.createHyperlinkButton("Gesamtsumme ermitteln", new ImageIcon(getClass().getResource("/artwork/22x22/bw/kcalc.png")), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    updateSummenAngabe();
                }
            });
            mypanel.add(gesamtSummeButton);

            cmbPast = new JComboBox();
            cmbPast.setModel(SYSCalendar.createMonthList(SYSCalendar.addField(SYSCalendar.today_date(), -2, GregorianCalendar.YEAR), SYSCalendar.today_date()));
            cmbPast.setSelectedIndex(cmbPast.getModel().getSize() - 1);
            cmbPast.setRenderer(new ListCellRenderer() {
                Format formatter = new SimpleDateFormat("MMMM yyyy");

                @Override
                public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
                    String text = formatter.format(o);
                    return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
                }
            });
            cmbPast.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    if (!ignoreDateComboEvent) {
                        updateSummenAngabe();
                    }
                }
            });
            cmbPast.setToolTipText("Monat für den die Gesamtsummenberechnung ermittelt werden soll");

            mypanel.add(cmbPast);
        }

        txtBW = new JXSearchField("Bewohnername oder Kennung");
        txtBW.setInstantSearchDelay(2000); // 2 Sekunden bevor der Caret Update zieht
        txtBW.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (txtBW.getText().trim().isEmpty()) {
                    return;
                }
                BewohnerTools.findeBW(txtBW.getText().trim(), new Closure() {
                    @Override
                    public void execute(Object o) {
                        if (o == null) {
                            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Keine(n) passende(n) Bewohner(in) gefunden.", 2));
                        } else {
                            bewohner = (Bewohner) o;
                            reloadDisplay();
                        }
                    }
                });
            }
        });

        mypanel.add(new JXTitledSeparator("Suchkriterien"));
        mypanel.add(txtBW);


        cmbVon = new JComboBox();

        cmbVon.setRenderer(new ListCellRenderer() {
            Format formatter = new SimpleDateFormat("MMMM yyyy");

            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
                String text = formatter.format(o);
                return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
            }
        });

        cmbVon.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (ignoreDateComboEvent) {
                    return;
                }
                if (cmbVon.getSelectedIndex() > cmbBis.getSelectedIndex()) {
                    ignoreDateComboEvent = true;
                    cmbBis.setSelectedIndex(cmbVon.getSelectedIndex());
                    ignoreDateComboEvent = false;
                }
                reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
            }
        });

        cmbVon.setToolTipText("Anzeigen der Belege ab welchem Monat ?");
        mypanel.add(new JXTitledSeparator("Zeitraum Von - Bis"));
//        mypanel.add(new TitledSeparator("Von", TitledSeparator.TYPE_PARTIAL_LINE, SwingConstants.LEADING));
        mypanel.add(cmbVon);


        cmbBis = new JComboBox();
        cmbBis.setRenderer(new ListCellRenderer() {
            Format formatter = new SimpleDateFormat("MMMM yyyy");

            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
                String text = formatter.format(o);
                return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
            }
        });

        cmbBis.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (ignoreDateComboEvent) {
                    return;
                }
                if (cmbVon.getSelectedIndex() > cmbBis.getSelectedIndex()) {
                    ignoreDateComboEvent = true;
                    cmbVon.setSelectedIndex(cmbBis.getSelectedIndex());
                    ignoreDateComboEvent = false;
                }
                reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
            }
        });

//        panelTime.add(new JLabel(" "));
//        mypanel.add(new TitledSeparator("Bis", TitledSeparator.TYPE_PARTIAL_GRADIENT_LINE, SwingConstants.LEADING));
        mypanel.add(cmbBis);
        cmbBis.setToolTipText("Anzeigen der Belege bis zu welchem Monat ?");

        cmbMonat = new JComboBox();
        cmbMonat.setRenderer(new ListCellRenderer() {
            Format formatter = new SimpleDateFormat("MMMM yyyy");

            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
//                OPDE.debug(o.toString());
                String text = formatter.format(o);
                return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
            }
        });

        cmbMonat.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                ignoreDateComboEvent = true;
                cmbVon.setSelectedItem(cmbMonat.getSelectedItem());
                cmbBis.setSelectedItem(cmbMonat.getSelectedItem());
                ignoreDateComboEvent = false;
                reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
            }
        });

//        panelTime.add(new JLabel(" "));
//        TitledSeparator ts = new TitledSeparator("Bestimmter Monat");

//        com.jidesoft.swing.PartialLineBorder
//        new PartialLineBorder(Color.WHITE, 1);
        mypanel.add(new JXTitledSeparator("Bestimmter Monat"));

        cmbMonat.setToolTipText("Anzeigen der Belege nur für einen bestimmten Monat");
        mypanel.add(cmbMonat);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setLayout(new HorizontalLayout());
        buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JButton homeButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_start.png")));
        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (cmbMonat.getSelectedIndex() > 0) {
                    cmbMonat.setSelectedIndex(0);
                }
            }
        });
        homeButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_start_pressed.png")));
        homeButton.setBorder(null);
        homeButton.setBorderPainted(false);
        homeButton.setOpaque(false);
        homeButton.setContentAreaFilled(false);

        JButton backButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_rev.png")));
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (cmbMonat.getSelectedIndex() > 0) {
                    cmbMonat.setSelectedIndex(cmbMonat.getSelectedIndex() - 1);
                }
            }
        });
        backButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_rev_pressed.png")));
        backButton.setBorder(null);
        backButton.setBorderPainted(false);
        backButton.setOpaque(false);
        backButton.setContentAreaFilled(false);


        JButton fwdButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_fwd.png")));
        fwdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (cmbMonat.getSelectedIndex() < cmbMonat.getModel().getSize() - 1) {
                    cmbMonat.setSelectedIndex(cmbMonat.getSelectedIndex() + 1);
                }
            }
        });
        fwdButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_fwd_pressed.png")));
        fwdButton.setBorder(null);
        fwdButton.setBorderPainted(false);
        fwdButton.setOpaque(false);
        fwdButton.setContentAreaFilled(false);

        JButton endButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_end.png")));
        endButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (cmbMonat.getSelectedIndex() < cmbMonat.getModel().getSize() - 1) {
                    cmbMonat.setSelectedIndex(cmbMonat.getModel().getSize() - 1);
                }
            }
        });
        endButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_end_pressed.png")));
        endButton.setBorder(null);
        endButton.setBorderPainted(false);
        endButton.setOpaque(false);
        endButton.setContentAreaFilled(false);


        buttonPanel.add(homeButton);
        buttonPanel.add(backButton);
        buttonPanel.add(fwdButton);
        buttonPanel.add(endButton);
        mypanel.add(buttonPanel);

        CollapsiblePane searchPane = new CollapsiblePane("Barbeträge");
        searchPane.setSlidingDirection(SwingConstants.SOUTH);
        searchPane.setStyle(CollapsiblePane.PLAIN_STYLE);
        searchPane.setCollapsible(false);
        searchPane.setContentPane(mypanel);

        searchPanes.add(searchPane);
        searchPanes.add(addBySearchBW());

        searchPanes.addExpansion();
        jspSearch.validate();
    }


    private CollapsiblePane addBySearchBW() {
        panelText = new CollapsiblePane("Bewohnerliste");

        JPanel mypanel = new JPanel();
        mypanel.setLayout(new VerticalLayout());
        mypanel.setBackground(Color.WHITE);
        searchSaldoButtonMap = new HashMap<Bewohner, Object[]>();

        EntityManager em = OPDE.createEM();

        Query query = em.createQuery(" " +
                " SELECT b, SUM(k.betrag) FROM Bewohner b " +
                " LEFT JOIN b.konto k " +
                " WHERE b.station IS NOT NULL " +
                " GROUP BY b " +
                " ORDER BY b.nachname, b.vorname, b.bWKennung ");

        List<Object[]> bwSearchList = query.getResultList();

        em.close();

        for (int row = 0; row < bwSearchList.size(); row++) {
            final Bewohner myBewohner = (Bewohner) bwSearchList.get(row)[0];
            BigDecimal saldo = bwSearchList.get(row)[1] == null ? BigDecimal.ZERO : (BigDecimal) bwSearchList.get(row)[1];

            String titel = "<html>" + myBewohner.getNachname() + ", " + myBewohner.getVorname() + " [" + myBewohner.getBWKennung() + "] <b><font " + (saldo.compareTo(BigDecimal.ZERO) < 0 ? "color=\"red\"" : "color=\"black\"") + ">" + currencyFormat.format(saldo) + "</font></b></html>";

            JideButton button = GUITools.createHyperlinkButton(titel, null, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    bewohner = myBewohner;
                    reloadDisplay();
                }
            });
            button.setButtonStyle(JideButton.FLAT_STYLE);
            searchSaldoButtonMap.put(myBewohner, new Object[]{saldo, button});
            mypanel.add(button);
        }

        try {
            panelText.setCollapsed(true);
        } catch (PropertyVetoException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        panelText.setSlidingDirection(SwingConstants.SOUTH);
        panelText.setStyle(CollapsiblePane.PLAIN_STYLE);
        panelText.setContentPane(mypanel);
        return panelText;
    }

    private void initSearchTime() {

        ignoreDateComboEvent = true;

        cmbVon.setModel(SYSCalendar.createMonthList(min, max));
        cmbVon.setSelectedIndex(cmbVon.getModel().getSize() - 1);

        cmbBis.setModel(SYSCalendar.createMonthList(min, max));
        cmbBis.setSelectedIndex(cmbBis.getModel().getSize() - 1);

        cmbMonat.setModel(SYSCalendar.createMonthList(min, max));
        cmbMonat.setSelectedIndex(cmbMonat.getModel().getSize() - 1);

        ignoreDateComboEvent = false;
    }

//    private CollapsiblePane addByTime() {
//        panelTime = new CollapsiblePane("nach Zeitraum");
//        JPanel innerpanel = new JPanel();
//        // new BoxLayout(innerpanel, BoxLayout.Y_AXIS)
//        innerpanel.setLayout(new VerticalLayout());
//
//        cmbVon = new JComboBox();
//
//        cmbVon.setRenderer(new ListCellRenderer() {
//            Format formatter = new SimpleDateFormat("MMMM yyyy");
//
//            @Override
//            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
//                String text = formatter.format(o);
//                return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
//            }
//        });
//
//        cmbVon.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent itemEvent) {
//                if (ignoreDateComboEvent) {
//                    return;
//                }
//                if (cmbVon.getSelectedIndex() > cmbBis.getSelectedIndex()) {
//                    ignoreDateComboEvent = true;
//                    cmbBis.setSelectedIndex(cmbVon.getSelectedIndex());
//                    ignoreDateComboEvent = false;
//                }
//                reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
//            }
//        });
//
//        innerpanel.add(new TitledSeparator("Von", TitledSeparator.TYPE_PARTIAL_GRADIENT_LINE, SwingConstants.LEADING));
//        innerpanel.add(cmbVon);
//
//
//        cmbBis = new JComboBox();
//        cmbBis.setRenderer(new ListCellRenderer() {
//            Format formatter = new SimpleDateFormat("MMMM yyyy");
//
//            @Override
//            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
//                String text = formatter.format(o);
//                return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
//            }
//        });
//
//        cmbBis.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent itemEvent) {
//                if (ignoreDateComboEvent) {
//                    return;
//                }
//                if (cmbVon.getSelectedIndex() > cmbBis.getSelectedIndex()) {
//                    ignoreDateComboEvent = true;
//                    cmbVon.setSelectedIndex(cmbBis.getSelectedIndex());
//                    ignoreDateComboEvent = false;
//                }
//                reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
//            }
//        });
//
////        panelTime.add(new JLabel(" "));
//        innerpanel.add(new TitledSeparator("Bis", TitledSeparator.TYPE_PARTIAL_GRADIENT_LINE, SwingConstants.LEADING));
//        innerpanel.add(cmbBis);
//
//        cmbMonat = new JComboBox();
//        cmbMonat.setRenderer(new ListCellRenderer() {
//            Format formatter = new SimpleDateFormat("MMMM yyyy");
//
//            @Override
//            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
////                OPDE.debug(o.toString());
//                String text = formatter.format(o);
//                return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
//            }
//        });
//
//        cmbMonat.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent itemEvent) {
//                ignoreDateComboEvent = true;
//                cmbVon.setSelectedItem(cmbMonat.getSelectedItem());
//                cmbBis.setSelectedItem(cmbMonat.getSelectedItem());
//                ignoreDateComboEvent = false;
//                reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
//            }
//        });
//
////        panelTime.add(new JLabel(" "));
//        innerpanel.add(new TitledSeparator("Bestimmter Monat", TitledSeparator.TYPE_PARTIAL_GRADIENT_LINE, SwingConstants.LEADING));
//        panelTime.setIcon(new ImageIcon(getClass().getResource("/artwork/22x22/date.png")));
//        innerpanel.add(cmbMonat);
//
//        JPanel buttonPanel = new JPanel();
//        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
//        buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
//
//        JButton homeButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_start.png")));
//        homeButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent actionEvent) {
//                if (cmbMonat.getSelectedIndex() > 0) {
//                    cmbMonat.setSelectedIndex(0);
//                }
//            }
//        });
////        homeButton.setBorder(new EmptyBorder(0,0,0,0));
//        JButton backButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/back.png")));
//        backButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent actionEvent) {
//                if (cmbMonat.getSelectedIndex() > 0) {
//                    cmbMonat.setSelectedIndex(cmbMonat.getSelectedIndex() - 1);
//                }
//            }
//        });
////        backButton.setBorder(new EmptyBorder(0,0,0,0));
//        JButton fwdButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/forward.png")));
//        fwdButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent actionEvent) {
//                if (cmbMonat.getSelectedIndex() < cmbMonat.getModel().getSize() - 1) {
//                    cmbMonat.setSelectedIndex(cmbMonat.getSelectedIndex() + 1);
//                }
//            }
//        });
////        fwdButton.setBorder(new EmptyBorder(0,0,0,0));
//        JButton endButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_end.png")));
//        endButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent actionEvent) {
//                if (cmbMonat.getSelectedIndex() < cmbMonat.getModel().getSize() - 1) {
//                    cmbMonat.setSelectedIndex(cmbMonat.getModel().getSize() - 1);
//                }
//            }
//        });
////        endButton.setBorder(new EmptyBorder(0,0,0,0));
//
//        buttonPanel.add(homeButton);
//        buttonPanel.add(backButton);
//        buttonPanel.add(fwdButton);
//        buttonPanel.add(endButton);
//        innerpanel.add(buttonPanel);
//
//
//        panelTime.setSlidingDirection(SwingConstants.SOUTH);
//        panelTime.setStyle(CollapsiblePane.TREE_STYLE);
//        panelTime.setContentPane(innerpanel);
//
//        panelTime.addCollapsiblePaneListener(new CollapsiblePaneAdapter() {
//            @Override
//            public void paneExpanded(CollapsiblePaneEvent collapsiblePaneEvent) {
//                reloadTable((Date) cmbVon.getSelectedItem(), (Date) cmbBis.getSelectedItem());
//            }
//
//            @Override
//            public void paneCollapsed(CollapsiblePaneEvent collapsiblePaneEvent) {
//                reloadTable();
//            }
//        });
//
//        return panelTime;
//    }

    private class CurrencyRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object value, boolean b, boolean b1, int i, int i1) {
            String text = value.toString();
            if (value instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) value;
                if (bd.compareTo(BigDecimal.ZERO) < 0) {
                    setForeground(Color.RED);
                } else {
                    setForeground(Color.BLACK);
                }

                NumberFormat nf = NumberFormat.getCurrencyInstance();
                text = nf.format(value);
                setHorizontalAlignment(JLabel.RIGHT);
            }
            return super.getTableCellRendererComponent(jTable, text, b, b1, i, i1);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    private Date checkDatum(String text, Date resetDate) {
        GregorianCalendar gc;
        Date result = resetDate;

        try {
            gc = SYSCalendar.erkenneDatum(text);
            if (SYSCalendar.sameDay(gc, SYSCalendar.today()) > 0) {
                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Sie haben ein Datum in der Zukunft eingegeben.", 2));
            } else {
                result = new Date(gc.getTimeInMillis());
            }
        } catch (NumberFormatException ex) {
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Sie haben ein falsches Datum eingegeben.", 2));

        }

        return result;
    }


    private BigDecimal checkBetrag(String text, BigDecimal defaultBetrag) {
        BigDecimal mybetrag = SYSTools.parseCurrency(text);
        if (mybetrag != null) {
            if (mybetrag.equals(BigDecimal.ZERO)) {
                OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Beträge mit '0,00 " + SYSConst.eurosymbol + "' werden nicht angenommen.", 2));
                mybetrag = defaultBetrag;
            }
        } else {
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage("Bitte geben Sie Euro Beträge in der folgenden Form ein: '10,0 " + SYSConst.eurosymbol + "'", 2));
//            lblMessage.setText(timeDF.format(new Date()) + " Uhr : " + "Bitte geben Sie Euro Beträge in der folgenden Form ein: '10,0 " + SYSConst.eurosymbol + "'");
            mybetrag = defaultBetrag;
        }
        return mybetrag;
    }

}
