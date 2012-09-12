package entity.prescription;

import entity.info.Resident;
import op.OPDE;
import op.tools.SYSConst;
import op.tools.SYSTools;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: tloehr
 * Date: 18.11.11
 * Time: 16:25
 * To change this template use File | Settings | File Templates.
 */
public class MedInventoryTools {

    public static ListCellRenderer getInventoryRenderer() {
        return new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected, boolean cellHasFocus) {
                String text;
                if (o == null) {
                    text = SYSTools.toHTML("<i>Keine Auswahl</i>");
                } else if (o instanceof MedInventory) {
                    MedInventory inventory = (MedInventory) o;
                    text = inventory.getText();
                } else {
                    text = o.toString();
                }
                return new DefaultListCellRenderer().getListCellRendererComponent(jList, text, i, isSelected, cellHasFocus);
            }
        };
    }

    public static String getInventoryAsHTML(MedInventory inventory) {
        String result = "";

        String htmlcolor = inventory.isClosed() ? "gray" : "blue";

        result += "<font face =\"" + SYSConst.ARIAL14.getFamily() + "\">";
        result += "<font color=\"" + htmlcolor + "\"><b><u>" + inventory.getID() + "</u></b></font>&nbsp; ";
        result += inventory.getText();


        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);

        result += "<br/><font color=\"blue\">Eingang: " + df.format(inventory.getFrom()) + "</font>";
        if (inventory.isClosed()) {
            result += "<br/><font color=\"green\">Abschluss: " + df.format(inventory.getTo()) + "</font>";
        }

        result += "</font>";
        return result;

    }

    public static BigDecimal getSum(MedInventory inventory) {
//        long timeStart = System.currentTimeMillis();
        BigDecimal result = BigDecimal.ZERO;
        for (MedStock bestand : inventory.getMedStocks()) {
            BigDecimal summe = MedStockTools.getSum(bestand);
            result = result.add(summe);
        }
//        long time2 = System.currentTimeMillis();
//        OPDE.debug("MedInventoryTools.getSumme(): " + (time2 - timeStart) + " millis");
        return result;
    }

    public static BigDecimal getSum(EntityManager em, MedInventory inventory) throws Exception {
//        long timeStart = System.currentTimeMillis();
        BigDecimal result = BigDecimal.ZERO;
        for (MedStock bestand : inventory.getMedStocks()) {
            BigDecimal summe = MedStockTools.getSum(em, bestand);
            result = result.add(summe);
        }
//        long time2 = System.currentTimeMillis();
//        OPDE.debug("MedInventoryTools.getSumme(): " + (time2 - timeStart) + " millis");
        return result;
    }

    /**
     * Bucht eine Menge aus einem Vorrat aus, ggf. zugehörig zu einer BHP. Übersteigt die Entnahme-Menge den
     * Restbestband, dann wird entweder
     * <ul>
     * <li>wenn der nächste Bestand <b>nicht</b> bekannt ist (<code>bestand.hashNext2Open() = <b>false</b></code>) &rarr; der Bestand trotzdem weiter gebucht. Bis ins negative.</li>
     * <li>wenn der nächste Bestand <b>doch schon</b> bekannt ist (<code>bestand.hashNext2Open() = <b>true</b></code>)  &rarr; ein neuer Bestand wird angebrochen.</li>
     * </ul>
     * Ist <b>keine</b> Packung im Anbruch, dann wird eine Exception geworfen. Das kann aber eingentlich nicht passieren. Es sei denn jemand hat von Hand
     * an den Datenbank rumgespielt.
     *
     * @param em         der EntityManager der verwendet wird
     * @param menge      die gewünschte Entnahmemenge
     * @param anweinheit true, dann wird in der Anwedungseinheit ausgebucht. false, in der Packungseinheit
     * @param bhp        BHP aufgrund dere dieser Buchungsvorgang erfolgt.
     */
    public static void takeFrom(EntityManager em, MedInventory inventory, BigDecimal menge, boolean anweinheit, BHP bhp) throws Exception {

        OPDE.debug("takeFrom/5: inventory: " + inventory);

        if (inventory == null) {
            throw new Exception("Keine Packung im Anbruch");
        }

        if (anweinheit) { // Umrechnung der Anwendungs Menge in die Packungs-Menge.
            MedStock bestand = MedStockTools.getStockInUse(inventory);
            BigDecimal apv = bestand.getAPV();

            if (apv.equals(BigDecimal.ZERO)) {
                apv = BigDecimal.ONE;
            }
            menge = menge.divide(apv, 4, BigDecimal.ROUND_UP);
        }

        OPDE.debug("takeFrom/5: menge: " + menge);

        takeFrom(em, inventory, menge, bhp);
    }


//    /**
//     * Die Rückgabe eines Vorrats bezieht sich auf eine BHP für die die Buchungen zurückgerechnet werden
//     * sollen.
//     * <ol>
//     * <li>Zuerst werden alle Buchungen zu einer BHPID herausgesucht.</li>
//     * <li>Gibt es mehr als eine, dann wurde f¸r die Buchung ein P?ckchen aufgebraucht und ein neues angefangen. In diesem Fall wird die Ausf¸hrung abgelehnt.</li>
//     * <li>Es werden alle zugeh?rigen Buchungen zu dieser BHPID gel?scht.</li>
//     * </ol>
//     *
//     * @param em  der EntityManager der verwendet wird
//     * @param bhp die BHP, die zurück genommen wird.
//     * @result true bei Erfolg, false sonst.
//     */
//    public static void rueckgabeVorrat(EntityManager em, BHP bhp) throws Exception {
//
//        Query delQuery = em.createQuery("DELETE FROM MedStockTransaction b WHERE b.bhp = :bhp");
//        delQuery.setParameter("bhp", bhp);
//
//        // Das hier passiert, wenn bei der Entnahme mehr als ein Bestand betroffen ist.
//        // Dann kann man das nicht mehr rückgängig machen. Und es wird eine Exception geworfen.
//        if (delQuery.executeUpdate() > 1) { // es gibt genau eine Buchung.
//            throw new Exception("Rueckgabe Vorrat");
//        }
//
//    }


    public static void takeFrom(EntityManager em, MedInventory inventory, BigDecimal wunschmenge, BHP bhp) throws Exception {
        MedStock bestand = em.merge(MedStockTools.getStockInUse(inventory));
        em.lock(bestand, LockModeType.OPTIMISTIC);

        OPDE.debug("takeFrom/4: bestand: " + bestand);

        if (bestand != null && wunschmenge.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal restsumme = MedStockTools.getSum(bestand); // wieviel der angebrochene Bestand noch hergibt.

            // normalerweise wird immer das hergegeben, was auch gewünscht ist. Notfalls bis ins minus.
            BigDecimal entnahme = wunschmenge; // wieviel in diesem Durchgang tatsächlich entnommen wird.


            /**
             * Sagen wir, dass in der Packung nicht mehr so viel drin ist, wie gewünscht.
             * Es gibt aber bereits eine NACHFOLGEPACKUNG. Dann brauchen wir DIESE hier
             * erst mal auf und nehmen den Rest aus der nächsten.
             *
             */
            if (bestand.hashNext2Open() && restsumme.compareTo(wunschmenge) <= 0) {
                entnahme = restsumme;
            }

            // Also erst mal die Buchung für DIESEN Durchgang.
            MedStockTransaction tx = em.merge(new MedStockTransaction(bestand, entnahme.negate(), bhp));
            bestand.getStockTransaction().add(tx);
            bhp.getStockTransaction().add(tx);
            OPDE.debug("takeFrom/4: tx: " + tx);

            /**
             * Gibt es schon eine neue Packung ? Wenn nicht, dann nehmen brechen wir ab.
             * So lange keine neue Packung bekannt nehmen wir immer weiter aus dieser hier.
             * Selbst wenn die dann ins Minus läuft.
             */
            if (bestand.hashNext2Open()) {
                if (restsumme.compareTo(wunschmenge) <= 0) { // ist nicht mehr genug in der Packung, bzw. die Packung wird jetzt leer.

                    MedStock naechsterBestand = em.merge(bestand.getNextStock());
                    em.lock(naechsterBestand, LockModeType.OPTIMISTIC);

                    // Es war mehr gewünscht, als die angebrochene Packung hergegeben hat.
                    // Bzw. die Packung wurde mit dieser Gabe geleert.
                    // Dann müssen wird erstmal den alten Bestand abschließen.
                    MedStockTools.close(em, bestand, "Automatischer Abschluss bei leerer Packung", MedStockTransactionTools.STATE_EDIT_EMPTY_SOON);
                }

                if (wunschmenge.compareTo(entnahme) > 0) { // Sind wir hier fertig, oder müssen wir noch mehr ausbuchen.
                    takeFrom(em, inventory, wunschmenge.subtract(entnahme), bhp);
                }
            }
        }
    }

    /**
     * Diese Methode bucht ein Medikament in einen Vorrat ein.
     * Dabei wird ein passendes APV ermittelt, eine Buchung angelegt und der neue MedBestand zurück gegeben.
     * Der muss dann nur noch persistiert werden.
     *
     * @param inventory
     * @param aPackage
     * @param darreichung
     * @param text
     * @param menge
     * @return
     * @throws Exception
     */
    public static MedStock addTo(MedInventory inventory, MedPackage aPackage, TradeForm darreichung, String text, BigDecimal menge) {
        MedStock bestand = null;
        if (menge.compareTo(BigDecimal.ZERO) > 0) {
            bestand = new MedStock(inventory, darreichung, aPackage, text);
            bestand.setAPV(MedStockTools.getAPV4(bestand));
            MedStockTransaction buchung = new MedStockTransaction(bestand, menge);
            bestand.getStockTransaction().add(buchung);
        }
        return bestand;
    }


    public static MedStock getNextToOpen(MedInventory inventory) {
        MedStock bestand = null;
        if (!inventory.getMedStocks().isEmpty()) {
            MedStock[] bestaende = inventory.getMedStocks().toArray(new MedStock[0]);
            Arrays.sort(bestaende); // nach Einbuchung
            for (MedStock myBestand : bestaende) {
                if (!myBestand.isOpened()) {
                    bestand = myBestand;
                    break;
                }
            }
        }
        return bestand;
    }

    public static MedStock getCurrentOpened(MedInventory inventory) {
        MedStock stock = null;
        if (!inventory.getMedStocks().isEmpty()) {
            for (MedStock mystock : inventory.getMedStocks()) {
                if (mystock.isOpened()) {
                    stock = mystock;
                    break;
                }
            }
        }
        return stock;
    }

//    public static MedStock getNaechsteNichtAbgeschlossene(MedInventory inventory) {
//        MedStock bestand = null;
//        if (!inventory.getMedStocks().isEmpty()) {
//            MedStock[] bestaende = inventory.getMedStocks().toArray(new MedStock[0]);
//            Arrays.sort(bestaende); // nach Einbuchung
//            for (MedStock myBestand : bestaende) {
//                if (!myBestand.isClosed()) {
//                    bestand = myBestand;
//                    break;
//                }
//            }
//        }
//        return bestand;
//    }

    /**
     * Bricht von allen geschlossenen das nächste (im Sinne des Einbuchdatums) an. Funktioniert nur bei Vorräten, die z.Zt. keine
     * angebrochenen Bestände haben.
     *
     * @param inventory
     * @return der neu angebrochene Bestand. null, wenns nicht geklappt hat.
     */
    public static MedStock anbrechenNaechste(MedInventory inventory) {
        MedStock result = null;


        java.util.List<MedStock> list = new ArrayList(inventory.getMedStocks());
        Collections.sort(list);

        for (MedStock bestand : list) {
            if (bestand.getOut().equals(SYSConst.DATE_BIS_AUF_WEITERES) && bestand.getOpened().equals(SYSConst.DATE_BIS_AUF_WEITERES)) {
                BigDecimal apv = MedStockTools.getAPV4(bestand);
                bestand.setOpened(new Date());
                bestand.setAPV(apv);
                result = bestand;
                break;
            }
        }

        return result;
    }

    public static DosageForm getForm(MedInventory inventory) {
        DosageForm form = null;
        try {

            EntityManager em = OPDE.createEM();

            OPDE.debug("MedInventoryTools.getForm: inventory: " + inventory.getID());

            Query query = em.createQuery(" " +
                    " SELECT tf.dosageForm FROM MedInventory i " +
                    " JOIN i.medStocks s " +
                    " JOIN s.tradeform tf" +
                    " WHERE i = :inventory");
            query.setParameter("inventory", inventory);
            query.setMaxResults(1);

            form = (DosageForm) query.getSingleResult();
            em.close();
        } catch (NoResultException nre) {
            form = null;
        } catch (Exception e){
            OPDE.fatal(e);
        }
        return form;
    }

    public static ArrayList<MedInventory> getAllActive(Resident resident) {
        ArrayList<MedInventory> result;

        EntityManager em = OPDE.createEM();
        Query query = em.createQuery("SELECT inv FROM MedInventory inv WHERE inv.resident = :resident AND inv.to = :to ORDER BY inv.text");
        query.setParameter("resident", resident);
        query.setParameter("to", SYSConst.DATE_BIS_AUF_WEITERES);

        result = new ArrayList<MedInventory>(query.getResultList());
        em.close();

        return result;
    }

    public static ArrayList<MedInventory> getAll(Resident resident) {
        ArrayList<MedInventory> result;

        EntityManager em = OPDE.createEM();
        Query query = em.createQuery("SELECT inv FROM MedInventory inv WHERE inv.resident = :resident ORDER BY inv.to DESC, inv.text");
        query.setParameter("resident", resident);

        result = new ArrayList<MedInventory>(query.getResultList());
        em.close();

        return result;
    }

}
