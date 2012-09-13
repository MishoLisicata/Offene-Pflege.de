package entity.prescription;

import op.OPDE;
import op.tools.SYSConst;
import op.tools.SYSTools;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: tloehr
 * Date: 01.12.11
 * Time: 17:17
 * To change this template use File | Settings | File Templates.
 */
public class DosageFormTools {
    public static final int APV1 = 0;
    public static final int APV_PER_DAF = 1;
    public static final int APV_PER_BW = 2;

    public static final String EINHEIT[] = {"", "Stück", "ml", "l", "mg", "g", "cm", "m"}; // Für AnwEinheit, PackEinheit, Dimension

    /**
     * @param maxlen maximale Zeichenlänge pro Zeile. maxlen < 1 heisst egal.
     * @return
     */
    public static ListCellRenderer getRenderer(int maxlen) {
        final int max = maxlen;
        return new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
                String text;
                if (o == null) {
                    text = SYSTools.toHTML("<i>" + OPDE.lang.getString("misc.commands.noselection") + "</i>");
                } else if (o instanceof DosageForm) {
                    DosageForm form = (DosageForm) o;
                    text = toPrettyString(form);
                } else {
                    text = o.toString();
                }
                if (max > 0) {
                    text = SYSTools.left(text, max);
                }
                return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
            }
        };
    }

    public static String getUsageText(DosageForm form) {
        String result = "";
        if (form == null) {
            result = "?";
        } else if (!form.getAnwText().isEmpty()) {
            result = form.getAnwText();
        } else {
            result = SYSConst.UNITS[form.getAnwEinheit()];
        }

        return result;
    }

    public static String toPrettyString(DosageForm form) {
        return (SYSTools.catchNull(form.getZubereitung()).isEmpty() ? form.getAnwText() : form.getZubereitung() + ", " + form.getAnwText());
    }

    public static String toPrettyStringPackung(DosageForm form) {
        String result = "";
        if (SYSTools.catchNull(form.getZubereitung()).isEmpty()) {
            result = EINHEIT[form.getPackEinheit()] + " " + form.getAnwText();
        } else {
            result = EINHEIT[form.getPackEinheit()] + " " + form.getZubereitung();
        }

        return result;
    }

}
