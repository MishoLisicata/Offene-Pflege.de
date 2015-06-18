/*
 * Created by JFormDesigner on Mon Jul 09 15:51:58 CEST 2012
 */

package op.residents.bwassistant;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.jidesoft.popup.JidePopup;
import entity.prescription.GP;
import entity.prescription.GPTools;
import op.residents.PnlEditGP;
import gui.GUITools;
import org.apache.commons.collections.Closure;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

/**
 * @author Torsten Löhr
 */
public class PnlGP extends JPanel {
    public static final String internalClassID = "opde.admin.bw.wizard.page4";
    private Closure validate;

    public PnlGP(Closure validate) {
        this.validate = validate;
        initComponents();
        initPanel();
    }

    private void initPanel() {
        ArrayList<GP> listGPs = GPTools.getAllActive();
        listGPs.add(0, null);

        cmbArzt.setModel(new DefaultComboBoxModel(listGPs.toArray()));
        cmbArzt.setRenderer(GPTools.getRenderer());
    }

    private void btnAddActionPerformed(ActionEvent e) {
        final JidePopup popupGP = GUITools.createPanelPopup(new PnlEditGP(new GP()), new Closure() {
            @Override
            public void execute(Object o) {
                if (o != null) {
                    cmbArzt.setModel(new DefaultComboBoxModel(new GP[]{(GP) o}));
                    validate.execute(cmbArzt.getSelectedItem());
                }
            }
        }, btnAdd);
        popupGP.setMovable(false);
        GUITools.showPopup(popupGP, SwingConstants.WEST);
    }

    private void cmbArztItemStateChanged(ItemEvent e) {
        validate.execute(cmbArzt.getSelectedItem());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        cmbArzt = new JComboBox();
        btnAdd = new JButton();

        //======== this ========
        setLayout(new FormLayout(
                "default:grow, $lcgap, default",
                "default"));

        //---- cmbArzt ----
        cmbArzt.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                cmbArztItemStateChanged(e);
            }
        });
        add(cmbArzt, CC.xy(1, 1));

        //---- btnAdd ----
        btnAdd.setText(null);
        btnAdd.setIcon(new ImageIcon(getClass().getResource("/artwork/22x22/bw/add.png")));
        btnAdd.setContentAreaFilled(false);
        btnAdd.setBorderPainted(false);
        btnAdd.setBorder(null);
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnAddActionPerformed(e);
            }
        });
        add(btnAdd, CC.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox cmbArzt;
    private JButton btnAdd;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
