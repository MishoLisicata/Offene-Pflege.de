/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package entity.vorgang;

import entity.*;
import entity.verordnungen.Verordnung;
import op.OPDE;
import op.tools.DlgException;
import op.tools.SYSConst;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

/**
 * @author tloehr
 */
public class VorgaengeTools {

    public static final int PDCA_OFF = 0;
    public static final int PDCA_PLAN = 1;
    public static final int PDCA_DO = 2;
    public static final int PDCA_CHECK = 3;
    public static final int PDCA_ACT = 4;
    public static final String[] PDCA = new String[]{"ABGESCHALTET", "Plan", "Do", "Check", "Act"};

    /**
     * Sucht alle Elemente für einen bestimmten Vorgang raus und gibt diesen als Liste von Elementen zurück.
     * Bei dieser Liste muss man beachten, dass sie aus zwei Arten von Elementen bestehen kann.
     * <ul>
     * <li>Entweder die Liste enthält einen (oder mehrere) Vorgangsberichte. Diese sind direkt per 1:n Relation mit den Vorgängen verbunden.</li>
     * <li>Oder es sind kleine Objekt Arrays (2-wertig), bei dem das erste Elemen (Index 0) das entsprechende VorgangElement (von welcher Art auch immer) enthält und das
     * zweite Element ist ein short, der den jeweiligen PDCA Zyklus enthält. Das liegt daran, dass die Zurordnungen der verschiedenen Dokumentationselemente über eine
     * attributierte m:n Relation erfolgt. Irgendwoher muss diese Information ja kommen.</li>
     * Daher muss in allen Dingen dieser Aufbau berücksichtigt werden. Das sieht schon bei dem elementsComparator.
     * </ul>
     *
     * @param vorgang
     * @return
     */
    public static List findElementeByVorgang(Vorgaenge vorgang, boolean mitSystem) {
        EntityManager em = OPDE.createEM();
        Comparator<Object> elementsComparator = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {

                long l1;
                if (o1 instanceof Object[]) {
                    l1 = ((VorgangElement) ((Object[]) o1)[0]).getPITInMillis();
                } else {
                    l1 = ((VorgangElement) o1).getPITInMillis();
                }


                long l2;
                if (o2 instanceof Object[]) {
                    l2 = ((VorgangElement) ((Object[]) o2)[0]).getPITInMillis();
                } else {
                    l2 = ((VorgangElement) o2).getPITInMillis();
                }

                return new Long(l1).compareTo(l2);
            }
        };

        List elements = new ArrayList();
        Query query;

        query = em.createNamedQuery(mitSystem ? "VBericht.findByVorgang" : "VBericht.findByVorgangOhneSystem");
        query.setParameter("vorgang", vorgang);
        elements.addAll(query.getResultList());

        query = em.createNamedQuery("Pflegeberichte.findByVorgang");
        query.setParameter("vorgang", vorgang);
        elements.addAll(query.getResultList());

        query = em.createNamedQuery("BWerte.findByVorgang");
        query.setParameter("vorgang", vorgang);
        elements.addAll(query.getResultList());

        query = em.createNamedQuery("Verordnung.findByVorgang");
        query.setParameter("vorgang", vorgang);
        elements.addAll(query.getResultList());

        query = em.createNamedQuery("BWInfo.findByVorgang");
        query.setParameter("vorgang", vorgang);
        elements.addAll(query.getResultList());

        query = em.createNamedQuery("Planung.findByVorgang");
        query.setParameter("vorgang", vorgang);
        elements.addAll(query.getResultList());

        Collections.sort(elements, elementsComparator);

        em.close();
        return elements;
    }

    public static void endVorgang(Vorgaenge vorgang) {
        EntityManager em = OPDE.createEM();
        try {
            em.getTransaction().begin();
            VBericht systemBericht = new VBericht("Vorgang abgeschlossen", VBerichtTools.VBERICHT_ART_CLOSE, vorgang);
            em.persist(systemBericht);
            vorgang.setBis(new Date());
            em.merge(vorgang);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            OPDE.fatal(e);
        } finally {
            em.close();
        }
    }

    public static void reopenVorgang(Vorgaenge vorgang) {
        EntityManager em = OPDE.createEM();
        try {
            em.getTransaction().begin();
            VBericht systemBericht = new VBericht("Vorgang wieder geöffnet", VBerichtTools.VBERICHT_ART_REOPEN, vorgang);
            em.persist(systemBericht);
            vorgang.setBis(SYSConst.DATE_BIS_AUF_WEITERES);
            em.merge(vorgang);
            em.getTransaction().commit();
        } catch (Exception ex) {
            em.getTransaction().rollback();
            new DlgException(ex);
        } finally {
            em.close();
        }
    }

    public static Vorgaenge createVorgang(String title, VKat vkat, Bewohner bw) {
        Vorgaenge vorgang = new Vorgaenge(title, bw, vkat);
        VBericht vbericht = new VBericht("Neuen Vorgang erstellt.", VBerichtTools.VBERICHT_ART_CREATE, vorgang);
        EntityManager em = OPDE.createEM();
        try {
            em.getTransaction().begin();
            em.persist(vorgang);
            em.persist(vbericht);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
        } finally {
            em.close();
        }
        return vorgang;
    }

    /**
     * Hängt ein VorgangElement an einen Vorgang an und erstellt einen entsprechenden SystemBericht dazu.
     *
     * @param element
     * @param vorgang
     */
    public static void add(VorgangElement element, Vorgaenge vorgang) {

        //TODO: Das hier muss völlig überarbeitet werden. Die Position muss anders ermittelt werden.

//
//        EntityManager em = OPDE.createEM();
//        try{
//        Object connectionObject = null;
//        String elementBezeichnung = "";
//        if (element instanceof Pflegeberichte) {
//            connectionObject = new SYSPB2VORGANG(vorgang, (Pflegeberichte) element);
//            elementBezeichnung = "Pflegebericht";
//        } else if (element instanceof BWerte) {
//            connectionObject = new SYSBWerte2VORGANG(vorgang, (BWerte) element);
//            elementBezeichnung = "Bewohner Wert";
//        } else if (element instanceof Verordnung) {
//            connectionObject = new SYSVER2VORGANG(vorgang, (Verordnung) element);
//            elementBezeichnung = "Ärztliche Verordnung";
//        } else if (element instanceof BWInfo) {
//            connectionObject = new SYSBWI2VORGANG(vorgang, (BWInfo) element);
//            elementBezeichnung = "Bewohner Information";
//        } else if (element instanceof Planung) {
//            connectionObject = new SYSPLAN2VORGANG(vorgang, (Planung) element);
//            elementBezeichnung = "Pflegeplanung";
//        } else {
//
//        }
//
//        em.persist(connectionObject);
//
//        // Jetzt fehlt nur noch eins: der PDCA Zyklus muss ermittelt werden. Dieser ergibt sich daraus, WO das Element einsortiert wurde. Daher fragen wir jetzt eine Gesamtübersicht ab.
//        // Der PDCA des neuen Elements ist der des vorherigen Elements. Ist das neue Element das erste in der Liste, ist der PDCA zwingend PLAN.
//
//        List alleElememente = findElementeByVorgang(vorgang, true);
//        int index = alleElememente.indexOf(element);
//        short pdca = PDCA_PLAN;
//        if (index > 0) {
//            Object o = alleElememente.get(index - 1);
//            if (o instanceof Object[]) {
//                pdca = ((Short) ((Object[]) o)[1]);
//            } else {
//                pdca = ((VBericht) o).getPdca();
//            }
//        }
//
//        // Connection Objekt korregieren
//        if (element instanceof Pflegeberichte) {
//            ((SYSPB2VORGANG) connectionObject).setPdca(pdca);
//        } else if (element instanceof BWerte) {
//            ((SYSBWerte2VORGANG) connectionObject).setPdca(pdca);
//        } else if (element instanceof Verordnung) {
//            ((SYSVER2VORGANG) connectionObject).setPdca(pdca);
//        } else if (element instanceof BWInfo) {
//            ((SYSBWI2VORGANG) connectionObject).setPdca(pdca);
//        } else if (element instanceof Planung) {
//            ((SYSPLAN2VORGANG) connectionObject).setPdca(pdca);
//        } else {
//
//        }
//        EntityTools.merge(connectionObject);
//
//        // Nun noch den Systembericht erstellen.
//        VBericht vbericht = new VBericht("Neue Zuordnung wurde vorgenommen für: " + elementBezeichnung + " ID: " + element.getID(), VBerichtTools.VBERICHT_ART_ASSIGN_ELEMENT, vorgang);
//        vbericht.setPdca(pdca);
//        EntityTools.persist(vbericht);
//
//        // Das ursprüngliche Element bekommt die Änderungen nicht mit. Und der JPA Cache auch nicht.
//        // Daher muss das Objekt hier manuell neu gelesen werden.
//        EntityTools.refresh(element);
    }

    /**
     * @param element
     * @param vorgang
     */
    public static void remove(VorgangElement element, Vorgaenge vorgang) {
//        EntityManager em = OPDE.createEM();
//        String elementBezeichnung = "";
//        Query query = null;
//        if (element instanceof Pflegeberichte) {
//            query = em.createNamedQuery("SYSPB2VORGANG.findByElementAndVorgang");
//            elementBezeichnung = "Pflegebericht";
//        } else if (element instanceof BWerte) {
//            query = em.createNamedQuery("SYSBWerte2VORGANG.findByElementAndVorgang");
//            elementBezeichnung = "Bewohner Wert";
//        } else if (element instanceof Verordnung) {
//            query = em.createNamedQuery("SYSVER2VORGANG.findByElementAndVorgang");
//            elementBezeichnung = "Ärztliche Verordnung";
//        } else if (element instanceof BWInfo) {
//            query = em.createNamedQuery("SYSBWI2VORGANG.findByElementAndVorgang");
//            elementBezeichnung = "Bewohner Information";
//        } else if (element instanceof Planung) {
//            query = em.createNamedQuery("SYSPLAN2VORGANG.findByElementAndVorgang");
//            elementBezeichnung = "Pflegeplanung";
//        } else {
//
//        }
//
//        query.setParameter("element", element);
//        query.setParameter("vorgang", vorgang);
//        connectionObjects = query.getSingleResult();
//
//
//        // Eigentlich sollte es nie mehr als einen dieser Objekte geben, aber dennoch.
//        Iterator it = connectionObjects.iterator();
//        while (it.hasNext()) {
//            Object obj = it.next();
//            short pdca = PDCA_OFF;
//            if (element instanceof Pflegeberichte) {
//                pdca = ((SYSPB2VORGANG) obj).getPdca();
//            } else {
//
//            }
//            EntityTools.delete(obj);
//            VBericht vbericht = new VBericht("Zuordnung entfernt für: " + elementBezeichnung + " ID: " + element.getID(), VBerichtTools.VBERICHT_ART_REMOVE_ELEMENT, vorgang);
//            vbericht.setPdca(pdca);
//            EntityTools.persist(vbericht);
//        }
//
//        // Das ursprüngliche Element bekommt die Änderungen nicht mit. Und der JPA Cache auch nicht.
//        // Daher muss das Objekt hier manuell neu gelesen werden.
//        EntityTools.refresh(element);
    }

    public static void setWVVorgang(Vorgaenge vorgang, Date wv) {

        EntityManager em = OPDE.createEM();

        try {
            em.getTransaction().begin();

            VBericht systemBericht = new VBericht("Wiedervorlage gesetzt auf: " + DateFormat.getDateInstance().format(wv), VBerichtTools.VBERICHT_ART_WV, vorgang);
            em.persist(systemBericht);

            vorgang.setWv(wv);
            em.merge(vorgang);

            em.getTransaction().commit();
        } catch (Exception ex) {
            em.getTransaction().rollback();
            OPDE.fatal(ex);
        } finally {
            em.close();
        }
    }

    /**
     * dreht den Kreislauf eine Stufe nach vorne.
     *
     * @param pdca
     * @return
     */
    public static short incPDCA(short pdca) {
        pdca++;
        if (pdca > PDCA_ACT) {
            pdca = PDCA_PLAN;
        }
        return pdca;
    }

    /**
     * dreht den Kreislauf eine Stufe zurück. Jedoch nicht weiter als PLAN.
     *
     * @param pdca
     * @return
     */
    public static short decPDCA(short pdca) {
        pdca--;
        if (pdca < PDCA_PLAN) {
            pdca = PDCA_PLAN;
        }
        return pdca;
    }

    private static JMenu getNeuMenu(VorgangElement element, Bewohner bewohner) {
        JMenu neu = new JMenu("Neu erstellen");
        final JTextField txt = new JTextField("");
        final Bewohner bw = bewohner;
        final VorgangElement finalElement = element;
        neu.add(txt);
        EntityManager em = OPDE.createEM();
        Query query = em.createNamedQuery("VKat.findAllSorted");

        Iterator<VKat> it = query.getResultList().iterator();
        while (it.hasNext()) {
            final VKat kat = it.next();
            JMenuItem mi = new JMenuItem(kat.getText());
            mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!txt.getText().trim().isEmpty()) {
                        Vorgaenge vorgang = createVorgang(txt.getText(), kat, bw);
                        add(finalElement, vorgang);
                        OPDE.debug("Vorgang '" + vorgang.getTitel() + "' für Bewohner '" + bw.getBWKennung() + "' angelegt. Element mit ID " + finalElement.getID() + " zugeordnet.");
                    }
                }
            });
            neu.add(mi);
        }

        em.close();

        return neu;
    }

    /**
     * Erstellt ein JMenu, dass zu einem bestimmten VorgangElement <code>element</code> und für einen bestimmten Bewohner <code>bewohner</code> alle Vorgänge enthält,
     * zu dem dieses Element <b>noch nicht</b> zuegordnet ist.
     *
     * @param element
     * @param bewohner
     * @return
     */
    private static JMenu getVorgaenge2Assign(VorgangElement element, Bewohner bewohner, ActionListener callback) {
        JMenu result = new JMenu("Zuordnen zu");
        EntityManager em = OPDE.createEM();
        final ActionListener cb = callback;

        final VorgangElement finalElement = element;

        // 1. Alle Vorgänge für den betreffenden BW suchen und in die Liste packen.
        List<Vorgaenge> vorgaenge = new ArrayList();
        Query query;
        query = em.createNamedQuery("Vorgaenge.findActiveByBewohner");
        query.setParameter("bewohner", bewohner);
        vorgaenge.addAll(query.getResultList());


        // 2. Alle die Vorgänge entfernen, zu denen das betreffenden Object bereits zugeordnet wurde.
        Query complement = null;
        if (element instanceof Pflegeberichte) {
            complement = em.createNamedQuery("SYSPB2VORGANG.findActiveAssignedVorgaengeByElement");
        } else if (element instanceof BWerte) {
            complement = em.createNamedQuery("SYSBWerte2VORGANG.findActiveAssignedVorgaengeByElement");
        } else if (element instanceof Verordnung) {
            complement = em.createNamedQuery("SYSVER2VORGANG.findActiveAssignedVorgaengeByElement");
        } else if (element instanceof BWInfo) {
            complement = em.createNamedQuery("SYSBWI2VORGANG.findActiveAssignedVorgaengeByElement");
        } else if (element instanceof Planung) {
            complement = em.createNamedQuery("SYSPLAN2VORGANG.findActiveAssignedVorgaengeByElement");
        } else {
            complement = null;
        }
        complement.setParameter("element", element);
        vorgaenge.removeAll(complement.getResultList());

        // 3. Nun alle Vorgänge als JMenuItems anhängen.
        Iterator<Vorgaenge> it = vorgaenge.iterator();
        while (it.hasNext()) {
            final Vorgaenge vorgang = it.next();
            JMenuItem mi = new JMenuItem(vorgang.getTitel());
            // Bei Aufruf eines Menüs, wird dass Element an den Vorgang angehangen.
            mi.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    add(finalElement, vorgang);
                    cb.actionPerformed(new ActionEvent(this, 0, "VorgangAssign"));
                }
            });
            result.add(mi);
        }
        em.close();
        return result;
    }

    private static JMenu getVorgaenge2Remove(VorgangElement element, ActionListener callback) {

        JMenu result = new JMenu("Entfernen von");

        final ActionListener cb = callback;

        final VorgangElement finalElement = element;
        EntityManager em = OPDE.createEM();

        // 1. Alle aktiven Vorgänge suchen, die diesem Element zugeordnet sind.
        List<Vorgaenge> vorgaenge = new ArrayList();
        Query query = null;
        if (element instanceof Pflegeberichte) {
            query = em.createNamedQuery("SYSPB2VORGANG.findActiveAssignedVorgaengeByElement");
        } else if (element instanceof BWerte) {
            query = em.createNamedQuery("SYSBWerte2VORGANG.findActiveAssignedVorgaengeByElement");
        } else if (element instanceof Verordnung) {
            query = em.createNamedQuery("SYSVER2VORGANG.findActiveAssignedVorgaengeByElement");
        } else if (element instanceof BWInfo) {
            query = em.createNamedQuery("SYSBWI2VORGANG.findActiveAssignedVorgaengeByElement");
        } else if (element instanceof Planung) {
            query = em.createNamedQuery("SYSPLAN2VORGANG.findActiveAssignedVorgaengeByElement");
        } else {
            query = null;
        }
        query.setParameter("element", element);
        vorgaenge.addAll(query.getResultList());

        // 2. Nun diese Vorgänge als JMenuItems anhängen.
        Iterator<Vorgaenge> it = vorgaenge.iterator();
        while (it.hasNext()) {
            final Vorgaenge vorgang = it.next();
            JMenuItem mi = new JMenuItem(vorgang.getTitel());
            // Bei Aufruf eines Menüs, wird dass Element vom Vorgang entfernt
            mi.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    remove(finalElement, vorgang);
                    cb.actionPerformed(new ActionEvent(this, 0, "VorgangRemove"));
                }
            });
            result.add(mi);
        }

        em.close();
        return result;
    }


    public static JMenu getVorgangContextMenu(Frame parent, VorgangElement element, Bewohner bewohner, ActionListener callback) {
        JMenu menu = new JMenu("<html>Vorgänge <font color=\"red\">&#9679;</font></html>");

        // Neuer Vorgang Menü
        menu.add(getNeuMenu(element, bewohner));

        // Untermenü mit vorhandenen Vorgängen einblenden.
        // Aber nur, wenn die nicht leer sind.
        JMenu addMenu = getVorgaenge2Assign(element, bewohner, callback);
        if (addMenu.getMenuComponentCount() > 0) {
            menu.add(addMenu);
        }
        JMenu delMenu = getVorgaenge2Remove(element, callback);
        if (delMenu.getMenuComponentCount() > 0) {
            menu.add(delMenu);
        }

        return menu;
    }


}