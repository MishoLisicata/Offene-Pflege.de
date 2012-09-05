/*
 * Created by JFormDesigner on Tue Sep 04 16:11:31 CEST 2012
 */

package op.allowance;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import entity.Allowance;
import entity.info.Resident;
import entity.info.ResidentTools;
import op.OPDE;
import op.threads.DisplayMessage;
import op.tools.GUITools;
import op.tools.SYSCalendar;
import op.tools.SYSTools;
import org.apache.commons.collections.Closure;
import org.joda.time.DateTime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

/**
 * @author Torsten Löhr
 */
public class PnlTX extends JPanel {
    public static final String internalClassID = "admin.residents.cash.pnltx";
    private Allowance tx;
    private Closure afterChange;
//    private JToggleButton tbSaveImmediately;

    public PnlTX(Allowance tx, Closure afterChange) {
        super();
        this.tx = tx;
        this.afterChange = afterChange;
        initComponents();
        initPanel();
    }

    private void txtDateFocusLost(FocusEvent evt) {
        DateTime dt;
        try {
            dt = new DateTime(SYSCalendar.parseDate(((JTextField) evt.getSource()).getText()));
        } catch (NumberFormatException ex) {
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.wrongdate")));
            dt = new DateTime();
        }
        if (dt.isAfterNow()) {
            dt = new DateTime();
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.futuredate")));
        }
        ((JTextField) evt.getSource()).setText(DateFormat.getDateInstance().format(dt.toDate()));
    }

    private void txtDateFocusGained(FocusEvent e) {
        txtDate.selectAll();
    }

    private void txtTextFocusGained(FocusEvent e) {
        txtText.selectAll();
    }

    private void txtCashFocusGained(FocusEvent e) {
        txtCash.selectAll();
    }

    private void txtTextFocusLost(FocusEvent e) {
        if (txtText.getText().trim().isEmpty()) {
            txtText.setText(OPDE.lang.getString(internalClassID + ".txtText"));
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.emptyentry")));
        }
    }

    private void txtDateActionPerformed(ActionEvent e) {
        txtText.requestFocus();
    }

    private void txtTextActionPerformed(ActionEvent e) {
        txtCash.requestFocus();
    }

    private void txtCashActionPerformed(ActionEvent e) {
//        if (!tbSaveImmediately.isSelected()) {
//            return;
//        }
        save();
        afterChange.execute(tx);
        tx = new Allowance((Resident) cmbResident.getSelectedItem());
    }

    private void save() {
        tx.setBearbeitetVon(OPDE.getLogin().getUser());
        tx.setBearbeitetAm(new Date());
        tx.setBelegDatum(SYSCalendar.parseDate(txtDate.getText()));
        tx.setBetrag(checkCash(txtCash.getText(), BigDecimal.ONE));
        tx.setBelegtext(txtText.getText().trim());
        tx.setResident((Resident) cmbResident.getSelectedItem());
    }

    private void txtCashFocusLost(FocusEvent e) {
        txtCash.setText(NumberFormat.getCurrencyInstance().format(checkCash(txtCash.getText(), BigDecimal.ONE)));
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        lblResident = new JLabel();
        cmbResident = new JComboBox();
        lblDate = new JLabel();
        txtDate = new JTextField();
        lblText = new JLabel();
        txtText = new JTextField();
        lblCash = new JLabel();
        txtCash = new JTextField();

        //======== this ========
        setLayout(new FormLayout(
            "default, $lcgap, pref, $lcgap, 161dlu, $lcgap, default",
            "default, $lgap, pref, 4*($lgap, default)"));

        //---- lblResident ----
        lblResident.setText("text");
        lblResident.setFont(new Font("Arial", Font.PLAIN, 14));
        add(lblResident, CC.xy(3, 3));

        //---- cmbResident ----
        cmbResident.setFont(new Font("Arial", Font.PLAIN, 14));
        add(cmbResident, CC.xy(5, 3));

        //---- lblDate ----
        lblDate.setText("text");
        lblDate.setFont(new Font("Arial", Font.PLAIN, 14));
        add(lblDate, CC.xy(3, 5));

        //---- txtDate ----
        txtDate.setFont(new Font("Arial", Font.PLAIN, 14));
        txtDate.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtDateFocusGained(e);
            }
            @Override
            public void focusLost(FocusEvent e) {
                txtDateFocusLost(e);
            }
        });
        txtDate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtDateActionPerformed(e);
            }
        });
        add(txtDate, CC.xy(5, 5));

        //---- lblText ----
        lblText.setText("text");
        lblText.setFont(new Font("Arial", Font.PLAIN, 14));
        add(lblText, CC.xy(3, 7));

        //---- txtText ----
        txtText.setFont(new Font("Arial", Font.PLAIN, 14));
        txtText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtTextFocusGained(e);
            }
            @Override
            public void focusLost(FocusEvent e) {
                txtTextFocusLost(e);
            }
        });
        txtText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtTextActionPerformed(e);
            }
        });
        add(txtText, CC.xy(5, 7));

        //---- lblCash ----
        lblCash.setText("text");
        lblCash.setFont(new Font("Arial", Font.PLAIN, 14));
        add(lblCash, CC.xy(3, 9));

        //---- txtCash ----
        txtCash.setFont(new Font("Arial", Font.PLAIN, 14));
        txtCash.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtCashFocusGained(e);
            }
            @Override
            public void focusLost(FocusEvent e) {
                txtCashFocusLost(e);
            }
        });
        txtCash.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtCashActionPerformed(e);
            }
        });
        add(txtCash, CC.xy(5, 9));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    private void initPanel() {
        lblResident.setText(OPDE.lang.getString(internalClassID + ".lblresident"));
        lblDate.setText(OPDE.lang.getString(internalClassID + ".lbldate"));
        lblText.setText(OPDE.lang.getString(internalClassID + ".lbltext"));
        lblCash.setText(OPDE.lang.getString(internalClassID + ".lblcash"));
        txtDate.setText(DateFormat.getDateInstance().format(new Date()));
        txtCash.setText("0,00");
        cmbResident.setModel(SYSTools.list2cmb(ResidentTools.getAllActive()));

//        tbSaveImmediately = GUITools.getNiceToggleButton(OPDE.lang.getString(internalClassID + ".tbSaveImmediately"));
//        add(tbSaveImmediately, CC.xywh(3, 11, 3, 1));
        txtText.setText(OPDE.lang.getString(internalClassID + ".txtText"));

        if (tx.getResident() != null) {
            cmbResident.setSelectedItem(tx.getResident());
            cmbResident.setEnabled(false);
        }

    }

    private BigDecimal checkCash(String text, BigDecimal defaultAmount) {
        BigDecimal myamount = SYSTools.parseCurrency(text);
        if (myamount != null) {
            if (myamount.equals(BigDecimal.ZERO)) {
                OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.emptycash")));
                myamount = defaultAmount;
            }
        } else {
            OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.wrongcash")));
            myamount = defaultAmount;
        }
        return myamount;
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel lblResident;
    private JComboBox cmbResident;
    private JLabel lblDate;
    private JTextField txtDate;
    private JLabel lblText;
    private JTextField txtText;
    private JLabel lblCash;
    private JTextField txtCash;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}