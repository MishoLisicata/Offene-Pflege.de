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
package op.care.bhp;

import com.jidesoft.pane.CollapsiblePane;
import com.jidesoft.pane.CollapsiblePanes;
import com.jidesoft.pane.event.CollapsiblePaneAdapter;
import com.jidesoft.pane.event.CollapsiblePaneEvent;
import com.jidesoft.popup.JidePopup;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.utils.ColorUtils;
import com.toedter.calendar.JDateChooser;
import entity.EntityTools;
import entity.info.Resident;
import entity.prescription.*;
import op.OPDE;
import op.system.InternalClassACL;
import op.threads.DisplayManager;
import op.threads.DisplayMessage;
import op.tools.*;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;
import org.joda.time.DateMidnight;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.*;

/**
 * @author tloehr
 */
public class PnlBHP extends NursingRecordsPanel {

    public static final String internalClassID = "nursingrecords.bhp";
    private Resident resident;
    private boolean initPhase;


    private Map<BHP, CollapsiblePane> mapBHP2Pane;
    private Map<Byte, ArrayList<BHP>> mapShift2BHP;
    private Map<Byte, CollapsiblePane> mapShift2Pane;
    private Map<Prescription, MedStock> mapPrescription2Stock;
    private int MAX_TEXT_LENGTH = 65;

    private JScrollPane jspSearch;
    private CollapsiblePanes searchPanes;
    private JDateChooser jdcDatum;

    public PnlBHP(Resident resident, JScrollPane jspSearch) {
        initComponents();
        this.jspSearch = jspSearch;
        initPanel();
        switchResident(resident);
    }

    @Override
    public String getInternalClassID() {
        return internalClassID;
    }

    @Override
    public void switchResident(Resident res) {
        this.resident = EntityTools.find(Resident.class, res.getRID());
        GUITools.setResidentDisplay(resident);

        initPhase = true;
        jdcDatum.setMinSelectableDate(BHPTools.getMinDatum(resident));
        jdcDatum.setDate(new Date());
        jdcDatum.setMaxSelectableDate(new Date());
        initPhase = false;

        reloadDisplay();
    }

    private void initPanel() {
        mapBHP2Pane = Collections.synchronizedMap(new HashMap<BHP, CollapsiblePane>());
        mapShift2Pane = Collections.synchronizedMap(new HashMap<Byte, CollapsiblePane>());
        mapShift2BHP = Collections.synchronizedMap(new HashMap<Byte, ArrayList<BHP>>());
        mapPrescription2Stock = Collections.synchronizedMap(new HashMap<Prescription, MedStock>());
        prepareSearchArea();
    }


    @Override
    public void cleanup() {
        jdcDatum.cleanup();
        synchronized (mapShift2BHP) {
            SYSTools.clear(mapShift2BHP);
        }
        synchronized (mapShift2Pane) {
            SYSTools.clear(mapShift2Pane);
        }
        synchronized (mapPrescription2Stock) {
            SYSTools.clear(mapPrescription2Stock);
        }
        synchronized (mapBHP2Pane) {
            SYSTools.clear(mapBHP2Pane);
        }
        SYSTools.unregisterListeners(this);
    }

    @Override
    public void reload() {
        reloadDisplay();
    }

    private void reloadDisplay() {
        /***
         *               _                 _ ____  _           _
         *      _ __ ___| | ___   __ _  __| |  _ \(_)___ _ __ | | __ _ _   _
         *     | '__/ _ \ |/ _ \ / _` |/ _` | | | | / __| '_ \| |/ _` | | | |
         *     | | |  __/ | (_) | (_| | (_| | |_| | \__ \ |_) | | (_| | |_| |
         *     |_|  \___|_|\___/ \__,_|\__,_|____/|_|___/ .__/|_|\__,_|\__, |
         *                                              |_|            |___/
         */
        initPhase = true;

        final boolean withworker = true;
        if (withworker) {

            OPDE.getMainframe().setBlocked(true);
            OPDE.getDisplayManager().setProgressBarMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.wait"), -1, 100));


            synchronized (mapPrescription2Stock) {
                SYSTools.clear(mapPrescription2Stock);
            }
            synchronized (mapBHP2Pane) {
                SYSTools.clear(mapBHP2Pane);
            }

            synchronized (mapShift2BHP) {
                if (mapShift2BHP != null) {
                    for (Byte key : mapShift2BHP.keySet()) {
                        mapShift2BHP.get(key).clear();
                    }
                    mapShift2BHP.clear();
                }
            }
            synchronized (mapShift2Pane) {
                if (mapShift2Pane != null) {
                    for (Byte key : mapShift2Pane.keySet()) {
                        mapShift2Pane.get(key).removeAll();
                    }
                    mapShift2Pane.clear();
                }
            }

            SwingWorker worker = new SwingWorker() {

                @Override
                protected Object doInBackground() throws Exception {

                    try {

                        int progress = 0;
                        OPDE.getDisplayManager().setProgressBarMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.wait"), -1, 100));

                        synchronized (mapShift2BHP) {
                            for (BHP bhp : BHPTools.getBHPs(resident, jdcDatum.getDate())) {
                                if (!mapShift2BHP.containsKey(bhp.getShift())) {
                                    mapShift2BHP.put(bhp.getShift(), new ArrayList<BHP>());
                                }
                                mapShift2BHP.get(bhp.getShift()).add(bhp);
                            }

                            if (!mapShift2BHP.containsKey(BHPTools.SHIFT_ON_DEMAND)) {
                                mapShift2BHP.put(BHPTools.SHIFT_ON_DEMAND, new ArrayList<BHP>());
                            }
                            mapShift2BHP.get(BHPTools.SHIFT_ON_DEMAND).addAll(BHPTools.getBHPsOnDemand(resident, jdcDatum.getDate()));
                        }

                        synchronized (mapShift2Pane) {
                            for (Byte shift : new Byte[]{BHPTools.SHIFT_ON_DEMAND, BHPTools.SHIFT_VERY_EARLY, BHPTools.SHIFT_EARLY, BHPTools.SHIFT_LATE, BHPTools.SHIFT_VERY_LATE}) {
                                mapShift2Pane.put(shift, createCP4(shift));
                                try {
                                    mapShift2Pane.get(shift).setCollapsed(shift == BHPTools.SHIFT_ON_DEMAND || shift != SYSCalendar.whatShiftIs(new Date()));
                                } catch (PropertyVetoException e) {
                                    OPDE.debug(e);
                                }
                                progress += 20;
                                OPDE.getDisplayManager().setProgressBarMessage(new DisplayMessage(OPDE.lang.getString("misc.msg.wait"), progress, 100));
                            }
                        }
                    } catch (Exception exc) {
                        OPDE.fatal(exc);
                    }

                    return null;
                }

                @Override
                protected void done() {
                    buildPanel(true);
                    initPhase = false;
                    OPDE.getDisplayManager().setProgressBarMessage(null);
                    OPDE.getMainframe().setBlocked(false);
                }
            };
            worker.execute();

        } else {

            mapBHP2Pane.clear();
            if (mapShift2BHP != null) {
                for (Byte key : mapShift2BHP.keySet()) {
                    mapShift2BHP.get(key).clear();
                }
                mapShift2BHP.clear();
            }
            if (mapShift2Pane != null) {
                for (Byte key : mapShift2Pane.keySet()) {
                    mapShift2Pane.get(key).removeAll();
                }
                mapShift2Pane.clear();
            }

            for (BHP bhp : BHPTools.getBHPs(resident, jdcDatum.getDate())) {
                if (!mapShift2BHP.containsKey(bhp.getShift())) {
                    mapShift2BHP.put(bhp.getShift(), new ArrayList<BHP>());
                }
                mapShift2BHP.get(bhp.getShift()).add(bhp);
            }

            if (!mapShift2BHP.containsKey(BHPTools.SHIFT_ON_DEMAND)) {
                mapShift2BHP.put(BHPTools.SHIFT_ON_DEMAND, new ArrayList<BHP>());
            }
            mapShift2BHP.get(BHPTools.SHIFT_ON_DEMAND).addAll(BHPTools.getBHPsOnDemand(resident, jdcDatum.getDate()));

            for (Byte shift : new Byte[]{BHPTools.SHIFT_ON_DEMAND, BHPTools.SHIFT_VERY_EARLY, BHPTools.SHIFT_EARLY, BHPTools.SHIFT_LATE, BHPTools.SHIFT_VERY_LATE}) {
                mapShift2Pane.put(shift, createCP4(shift));
                try {
                    mapShift2Pane.get(shift).setCollapsed(shift == BHPTools.SHIFT_ON_DEMAND || shift != SYSCalendar.whatShiftIs(new Date()));
                } catch (PropertyVetoException e) {
                    OPDE.debug(e);
                }
            }

            buildPanel(true);

        }
        initPhase = false;
    }

    private void buildPanel(boolean resetCollapseState) {
        synchronized (mapShift2Pane) {
            cpBHP.removeAll();
            cpBHP.setLayout(new JideBoxLayout(cpBHP, JideBoxLayout.Y_AXIS));
            for (Byte shift : new Byte[]{BHPTools.SHIFT_ON_DEMAND, BHPTools.SHIFT_VERY_EARLY, BHPTools.SHIFT_EARLY, BHPTools.SHIFT_LATE, BHPTools.SHIFT_VERY_LATE}) {
                OPDE.debug(shift);
                cpBHP.add(mapShift2Pane.get(shift));
                if (resetCollapseState) {
                    try {
                        mapShift2Pane.get(shift).setCollapsed(shift != SYSCalendar.whatShiftIs(new Date()));
                    } catch (PropertyVetoException e) {
                        OPDE.debug(e);
                    }
                }
            }
        }
        cpBHP.addExpansion();
    }

    private CollapsiblePane createCP4(Byte shift) {
        if (shift == BHPTools.SHIFT_ON_DEMAND) {
            return createCP4OnDemand();
        } else {
            return createCP4Shift(shift);
        }
    }

    private CollapsiblePane createCP4OnDemand() {
        /***
         *                          _        ____ ____  _  _    ___        ____                                 _
         *       ___ _ __ ___  __ _| |_ ___ / ___|  _ \| || |  / _ \ _ __ |  _ \  ___ _ __ ___   __ _ _ __   __| |
         *      / __| '__/ _ \/ _` | __/ _ \ |   | |_) | || |_| | | | '_ \| | | |/ _ \ '_ ` _ \ / _` | '_ \ / _` |
         *     | (__| | |  __/ (_| | ||  __/ |___|  __/|__   _| |_| | | | | |_| |  __/ | | | | | (_| | | | | (_| |
         *      \___|_|  \___|\__,_|\__\___|\____|_|      |_|  \___/|_| |_|____/ \___|_| |_| |_|\__,_|_| |_|\__,_|
         *
         */
        String title = "<html><font size=+1><b>" + OPDE.lang.getString(internalClassID + ".ondemand") + "</b></font></html>";

        final CollapsiblePane mainPane = new CollapsiblePane(title);
        mainPane.setSlidingDirection(SwingConstants.SOUTH);
        mainPane.setBackground(SYSCalendar.getBGSHIFT(BHPTools.SHIFT_ON_DEMAND));
        mainPane.setForeground(SYSCalendar.getFGSHIFT(BHPTools.SHIFT_ON_DEMAND));
        mainPane.setOpaque(false);

        if (!mapShift2BHP.get(BHPTools.SHIFT_ON_DEMAND).isEmpty()) {
            Prescription currentPrescription = null;
            CollapsiblePane sitPane = null;
            JPanel sitPanel = null;
            JPanel sitOuterPanel = new JPanel();
            sitOuterPanel.setLayout(new VerticalLayout());
            for (BHP bhp : mapShift2BHP.get(BHPTools.SHIFT_ON_DEMAND)) {
                if (currentPrescription == null || bhp.getPrescription().getID() != currentPrescription.getID()) {
                    if (currentPrescription != null) {
                        sitPane.setContentPane(sitPanel);
                        sitOuterPanel.add(sitPane);
                    }
                    currentPrescription = bhp.getPrescription();
                    sitPanel = new JPanel();
                    sitPanel.setLayout(new VerticalLayout());
                    sitPanel.setBackground(bhp.getBG());
                    sitPane = new CollapsiblePane(SYSTools.toHTMLForScreen("<b>" + currentPrescription.getSituation().getText()) + "</b>");
                    sitPane.setSlidingDirection(SwingConstants.SOUTH);
                    sitPane.setBackground(ColorUtils.getDerivedColor(SYSCalendar.getBGSHIFT(BHPTools.SHIFT_ON_DEMAND), 0.4f)); // a little darker
                    sitPane.setForeground(Color.BLACK);
                    sitPane.setOpaque(false);
                }

                sitPane.setContentPane(sitPanel);
                sitOuterPanel.add(sitPane);

                mapBHP2Pane.put(bhp, createCP4(bhp));
                sitPanel.add(mapBHP2Pane.get(bhp));
            }
            mainPane.setContentPane(sitOuterPanel);
            mainPane.setCollapsible(true);
        } else {
            mainPane.setContentPane(new JPanel());
            mainPane.setCollapsible(false);
        }

        return mainPane;
    }

    private CollapsiblePane createCP4Shift(final Byte shift) {
        /***
         *                          _        ____ ____  _  _
         *       ___ _ __ ___  __ _| |_ ___ / ___|  _ \| || |
         *      / __| '__/ _ \/ _` | __/ _ \ |   | |_) | || |_
         *     | (__| | |  __/ (_| | ||  __/ |___|  __/|__   _|
         *      \___|_|  \___|\__,_|\__\___|\____|_|      |_|
         *
         */
        String title = "<html><font size=+1><b>" + GUITools.getLocalizedMessages(BHPTools.SHIFT_TEXT)[shift] + "</b></font></html>";

        final CollapsiblePane prPane = new CollapsiblePane(title);
        prPane.setSlidingDirection(SwingConstants.SOUTH);
        prPane.setBackground(SYSCalendar.getBGSHIFT(shift));
        prPane.setForeground(SYSCalendar.getFGSHIFT(shift));
        prPane.setOpaque(false);

        JPanel prPanel = new JPanel();
        prPanel.setLayout(new VerticalLayout());

        if (mapShift2BHP.containsKey(shift)) {

            if (shift == BHPTools.SHIFT_EARLY) {

                final CollapsiblePane morning = new CollapsiblePane("<html><font size=+1>" + OPDE.lang.getString("misc.msg.morning.long") + "</font></html>", null);
                morning.setSlidingDirection(SwingConstants.SOUTH);
                morning.setBackground(SYSCalendar.getBGSHIFT(shift).darker());
                morning.setForeground(Color.white);
                morning.setOpaque(false);
                JPanel pnlMorning = new JPanel();
                pnlMorning.setLayout(new VerticalLayout());
                morning.setContentPane(pnlMorning);

                final CollapsiblePane noon = new CollapsiblePane("<html><font size=+1>" + OPDE.lang.getString("misc.msg.noon.long") + "</font></html>", null);
                noon.setSlidingDirection(SwingConstants.SOUTH);
                noon.setBackground(SYSCalendar.getBGSHIFT(shift).darker());
                noon.setForeground(Color.white);
                noon.setOpaque(false);
                JPanel pnlNoon = new JPanel();
                pnlNoon.setLayout(new VerticalLayout());
                noon.setContentPane(pnlNoon);

                final CollapsiblePane clock = new CollapsiblePane("<html><font size=+1>" + OPDE.lang.getString("misc.msg.Time.long") + "</font></html>", null);
                clock.setSlidingDirection(SwingConstants.SOUTH);
                clock.setBackground(SYSCalendar.getBGSHIFT(shift).darker());
                clock.setForeground(Color.white);
                clock.setOpaque(false);
                JPanel pnlClock = new JPanel();
                pnlClock.setLayout(new VerticalLayout());
                clock.setContentPane(pnlClock);

                for (BHP bhp : mapShift2BHP.get(shift)) {
                    prPanel.setBackground(bhp.getBG());
                    mapBHP2Pane.put(bhp, createCP4(bhp));
                    if (bhp.getSollZeit() == BHPTools.BYTE_MORNING) {
                        pnlMorning.add(mapBHP2Pane.get(bhp));
                    } else if (bhp.getSollZeit() == BHPTools.BYTE_NOON) {
                        pnlNoon.add(mapBHP2Pane.get(bhp));
                    } else {
                        pnlClock.add(mapBHP2Pane.get(bhp));
                    }
                }

                if (pnlClock.getComponentCount() > 0) {
                    prPanel.add(clock);
                }
                if (pnlMorning.getComponentCount() > 0) {
                    prPanel.add(morning);
                }
                if (pnlNoon.getComponentCount() > 0) {
                    prPanel.add(noon);
                }

            } else if (shift == BHPTools.SHIFT_LATE) {
                final CollapsiblePane afternoon = new CollapsiblePane("<html><font size=+1>" + OPDE.lang.getString("misc.msg.afternoon.long") + "</font></html>", null);
                afternoon.setSlidingDirection(SwingConstants.SOUTH);
                afternoon.setBackground(SYSCalendar.getBGSHIFT(shift).darker());
                afternoon.setForeground(Color.white);
                afternoon.setOpaque(false);
                JPanel pnlAfternoon = new JPanel();
                pnlAfternoon.setLayout(new VerticalLayout());
                afternoon.setContentPane(pnlAfternoon);

                final CollapsiblePane evening = new CollapsiblePane("<html><font size=+1>" + OPDE.lang.getString("misc.msg.evening.long") + "</font></html>", null);
                evening.setSlidingDirection(SwingConstants.SOUTH);
                evening.setBackground(SYSCalendar.getBGSHIFT(shift).darker());
                evening.setForeground(Color.white);
                evening.setOpaque(false);
                JPanel pnlEvening = new JPanel();
                pnlEvening.setLayout(new VerticalLayout());
                evening.setContentPane(pnlEvening);

                final CollapsiblePane clock = new CollapsiblePane("<html><font size=+1>" + OPDE.lang.getString("misc.msg.Time.long") + "</font></html>", null);
                clock.setSlidingDirection(SwingConstants.SOUTH);
                clock.setBackground(SYSCalendar.getBGSHIFT(shift).darker());
                clock.setForeground(Color.white);
                clock.setOpaque(false);
                JPanel pnlClock = new JPanel();
                pnlClock.setLayout(new VerticalLayout());
                clock.setContentPane(pnlClock);

                for (BHP bhp : mapShift2BHP.get(shift)) {
                    prPanel.setBackground(bhp.getBG());
                    mapBHP2Pane.put(bhp, createCP4(bhp));
                    if (bhp.getSollZeit() == BHPTools.BYTE_AFTERNOON) {
                        pnlAfternoon.add(mapBHP2Pane.get(bhp));
                    } else if (bhp.getSollZeit() == BHPTools.BYTE_EVENING) {
                        pnlEvening.add(mapBHP2Pane.get(bhp));
                    } else {
                        pnlClock.add(mapBHP2Pane.get(bhp));
                    }
                }

                if (pnlClock.getComponentCount() > 0) {
                    prPanel.add(clock);
                }
                if (pnlAfternoon.getComponentCount() > 0) {
                    prPanel.add(afternoon);
                }
                if (pnlEvening.getComponentCount() > 0) {
                    prPanel.add(evening);
                }
            } else {
                for (BHP bhp : mapShift2BHP.get(shift)) {
                    prPanel.setBackground(bhp.getBG());
                    mapBHP2Pane.put(bhp, createCP4(bhp));
                    prPanel.add(mapBHP2Pane.get(bhp));
                }
            }
            prPane.setContentPane(prPanel);
            prPane.setCollapsible(true);
        } else {
            prPane.setContentPane(prPanel);
            prPane.setCollapsible(false);
        }

        return prPane;
    }

    private CollapsiblePane createCP4(final BHP bhp) {
        final CollapsiblePane bhpPane = new CollapsiblePane();
        bhpPane.setCollapseOnTitleClick(false);


        ActionListener applyActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (bhp.getStatus() != BHPTools.STATE_OPEN) {
                    return;
                }
                if (bhp.getPrescription().isClosed()) {
                    return;
                }

                if (BHPTools.isChangeable(bhp)) {
                    EntityManager em = OPDE.createEM();
                    try {
                        em.getTransaction().begin();

                        em.lock(em.merge(resident), LockModeType.OPTIMISTIC);
                        BHP myBHP = em.merge(bhp);
                        em.lock(myBHP, LockModeType.OPTIMISTIC);

                        if (myBHP.isOnDemand()) {
                            em.lock(myBHP.getPrescriptionSchedule(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                            em.lock(myBHP.getPrescription(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                        } else {
                            em.lock(myBHP.getPrescriptionSchedule(), LockModeType.OPTIMISTIC);
                            em.lock(myBHP.getPrescription(), LockModeType.OPTIMISTIC);
                        }

                        myBHP.setStatus(BHPTools.STATE_DONE);
                        myBHP.setUser(em.merge(OPDE.getLogin().getUser()));
                        myBHP.setIst(new Date());
                        myBHP.setiZeit(SYSCalendar.whatTimeIDIs(new Date()));
                        myBHP.setMDate(new Date());

                        Prescription involvedPresciption = null;
                        if (myBHP.shouldBeCalculated()) {
                            MedInventory inventory = TradeFormTools.getInventory4TradeForm(resident, myBHP.getTradeForm());
                            MedInventoryTools.withdraw(em, em.merge(inventory), myBHP.getDose(), myBHP);
                            // Was the prescription closed during this withdraw ?
                            involvedPresciption = em.find(Prescription.class, myBHP.getPrescription().getID());
                        }
                        em.getTransaction().commit();


                        if (myBHP.shouldBeCalculated() && involvedPresciption.isClosed()) { // &&
                            reload();
                        } else {
                            mapBHP2Pane.put(myBHP, createCP4(myBHP));
                            int position = mapShift2BHP.get(myBHP.getShift()).indexOf(bhp);
                            mapShift2BHP.get(myBHP.getShift()).remove(position);
                            mapShift2BHP.get(myBHP.getShift()).add(position, myBHP);
                            if (myBHP.isOnDemand()) {
                                // This whole thing here is only to handle the BPHs on Demand
                                // Fix the other BHPs on demand. If not, you will get locking excpetions,
                                // we FORCED INCREMENTED LOCKS on the Schedule and the Prescription.
                                ArrayList<BHP> changeList = new ArrayList<BHP>();
                                for (BHP bhp : mapShift2BHP.get(BHPTools.SHIFT_ON_DEMAND)) {
                                    if (bhp.getPrescription().getID() == myBHP.getPrescription().getID() && bhp.getBHPid() != myBHP.getBHPid()) {
                                        bhp.setPrescription(myBHP.getPrescription());
                                        bhp.setPrescriptionSchedule(myBHP.getPrescriptionSchedule());
                                        changeList.add(bhp);
                                    }
                                }
                                for (BHP bhp : changeList) {
                                    mapBHP2Pane.put(bhp, createCP4(myBHP));
                                    position = mapShift2BHP.get(bhp.getShift()).indexOf(bhp);
                                    mapShift2BHP.get(myBHP.getShift()).remove(position);
                                    mapShift2BHP.get(myBHP.getShift()).add(position, bhp);
                                }

                                Collections.sort(mapShift2BHP.get(myBHP.getShift()), BHPTools.getOnDemandComparator());
                            } else {
                                Collections.sort(mapShift2BHP.get(myBHP.getShift()));
                            }

                            mapShift2Pane.put(myBHP.getShift(), createCP4(myBHP.getShift()));
                            buildPanel(false);
                        }
                    } catch (OptimisticLockException ole) {
                        OPDE.warn(ole);
                        OPDE.warn(ole);
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
                            OPDE.getMainframe().emptyFrame();
                            OPDE.getMainframe().afterLogin();
                        }
                        OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                    } catch (RollbackException ole) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
                            OPDE.getMainframe().emptyFrame();
                            OPDE.getMainframe().afterLogin();
                        }
                        OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                    } catch (Exception e) {
                        if (em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        OPDE.fatal(e);
                    } finally {
                        em.close();
                    }

                } else {
                    OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString(internalClassID + ".notchangeable")));
                }
            }
        };


//        JPanel titlePanelleft = new JPanel();
//        titlePanelleft.setLayout(new BoxLayout(titlePanelleft, BoxLayout.LINE_AXIS));

        MedStock stock = mapPrescription2Stock.get(bhp.getPrescription());
        if (bhp.hasMed() && stock == null) {
            stock = MedStockTools.getStockInUse(TradeFormTools.getInventory4TradeForm(resident, bhp.getTradeForm()));
            mapPrescription2Stock.put(bhp.getPrescription(), stock);
        }

        String title = "<html><font size=+1>" +
                SYSTools.left(PrescriptionTools.getShortDescriptionAsCompactText(bhp.getPrescriptionSchedule().getPrescription()), MAX_TEXT_LENGTH) +
                (bhp.hasMed() ? ", <b>" + SYSTools.getAsHTML(bhp.getDose()) +
                        " " + DosageFormTools.getUsageText(bhp.getPrescription().getTradeForm().getDosageForm()) + "</b>" : "") +
                BHPTools.getScheduleText(bhp, ", ", "") +
                (bhp.getUser() != null ? ", <i>" + SYSTools.anonymizeUser(bhp.getUser().getUID()) + "</i>" : "") +
                "</font></html>";


        DefaultCPTitle cptitle = new DefaultCPTitle(title, OPDE.getAppInfo().isAllowedTo(InternalClassACL.UPDATE, internalClassID) ? applyActionListener : null);


        JLabel icon1 = new JLabel(BHPTools.getIcon(bhp));
        icon1.setOpaque(false);
        JLabel icon2 = new JLabel(BHPTools.getWarningIcon(bhp, stock));
        icon2.setOpaque(false);


        cptitle.getAdditionalIconPanel().add(icon1);
        cptitle.getAdditionalIconPanel().add(icon2);

        if (bhp.getPrescription().isClosed()) {
            JLabel icon3 = new JLabel(SYSConst.icon22stopSign);
            icon3.setOpaque(false);
            cptitle.getAdditionalIconPanel().add(icon3);
        }


        if (OPDE.getAppInfo().isAllowedTo(InternalClassACL.UPDATE, internalClassID)) {
            if (!bhp.getPrescription().isClosed()) {

                /***
                 *      _     _            _                _
                 *     | |__ | |_ _ __    / \   _ __  _ __ | |_   _
                 *     | '_ \| __| '_ \  / _ \ | '_ \| '_ \| | | | |
                 *     | |_) | |_| | | |/ ___ \| |_) | |_) | | |_| |
                 *     |_.__/ \__|_| |_/_/   \_\ .__/| .__/|_|\__, |
                 *                             |_|   |_|      |___/
                 */
                JButton btnApply = new JButton(SYSConst.icon22apply);
                btnApply.setPressedIcon(SYSConst.icon22applyPressed);
                btnApply.setAlignmentX(Component.RIGHT_ALIGNMENT);
                btnApply.setToolTipText(OPDE.lang.getString(internalClassID + ".btnApply.tooltip"));
                btnApply.addActionListener(applyActionListener);
                btnApply.setContentAreaFilled(false);
                btnApply.setBorder(null);
                btnApply.setEnabled(bhp.isOpen() && (!bhp.hasMed() || mapPrescription2Stock.containsKey(bhp.getPrescription())));
                cptitle.getRight().add(btnApply);
//                JPanel spacer = new JPanel();
//                spacer.setOpaque(false);
//                cptitle.getRight().add(spacer);

                /***
                 *                             ____  _             _
                 *       ___  _ __   ___ _ __ / ___|| |_ ___   ___| | __
                 *      / _ \| '_ \ / _ \ '_ \\___ \| __/ _ \ / __| |/ /
                 *     | (_) | |_) |  __/ | | |___) | || (_) | (__|   <
                 *      \___/| .__/ \___|_| |_|____/ \__\___/ \___|_|\_\
                 *           |_|
                 */
                if (bhp.hasMed() && stock == null && MedInventoryTools.getNextToOpen(TradeFormTools.getInventory4TradeForm(resident, bhp.getTradeForm())) != null) {
                    final JButton btnOpenStock = new JButton(SYSConst.icon22ledGreenOn);
                    btnOpenStock.setPressedIcon(SYSConst.icon22ledGreenOff);
                    btnOpenStock.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    btnOpenStock.setContentAreaFilled(false);
                    btnOpenStock.setBorder(null);
                    btnOpenStock.setToolTipText(SYSTools.toHTMLForScreen(OPDE.lang.getString("nursingrecords.inventory.stock.btnopen.tooltip")));
                    btnOpenStock.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {

                            EntityManager em = OPDE.createEM();
                            try {
                                em.getTransaction().begin();
                                em.lock(em.merge(resident), LockModeType.OPTIMISTIC);
                                BHP myBHP = em.merge(bhp);
                                em.lock(myBHP, LockModeType.OPTIMISTIC);
                                em.lock(myBHP.getPrescriptionSchedule(), LockModeType.OPTIMISTIC);
                                em.lock(myBHP.getPrescription(), LockModeType.OPTIMISTIC);

                                MedStock myStock = em.merge(MedInventoryTools.openNext(TradeFormTools.getInventory4TradeForm(resident, myBHP.getTradeForm())));
                                em.lock(myStock, LockModeType.OPTIMISTIC);
                                em.getTransaction().commit();

                                OPDE.getDisplayManager().addSubMessage(new DisplayMessage(String.format(OPDE.lang.getString("newstocks.stock.has.been.opened"), myStock.getID().toString())));
                                reload();

                            } catch (OptimisticLockException ole) {
                                OPDE.warn(ole);
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();
                                }
                                if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
                                    OPDE.getMainframe().emptyFrame();
                                    OPDE.getMainframe().afterLogin();
                                }
                                OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            } catch (Exception e) {
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();
                                }
                                OPDE.fatal(e);
                            } finally {
                                em.close();
                            }


                        }


                    });
                    cptitle.getRight().add(btnOpenStock);
                }

                /***
                 *      _     _         ____       __
                 *     | |__ | |_ _ __ |  _ \ ___ / _|_   _ ___  ___
                 *     | '_ \| __| '_ \| |_) / _ \ |_| | | / __|/ _ \
                 *     | |_) | |_| | | |  _ <  __/  _| |_| \__ \  __/
                 *     |_.__/ \__|_| |_|_| \_\___|_|  \__,_|___/\___|
                 *
                 */
                final JButton btnRefuse = new JButton(SYSConst.icon22cancel);
                btnRefuse.setPressedIcon(SYSConst.icon22cancelPressed);
                btnRefuse.setAlignmentX(Component.RIGHT_ALIGNMENT);
                btnRefuse.setContentAreaFilled(false);
                btnRefuse.setBorder(null);
                btnRefuse.setToolTipText(SYSTools.toHTMLForScreen(OPDE.lang.getString(internalClassID + ".btnRefuse.tooltip")));
                btnRefuse.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (bhp.getStatus() != BHPTools.STATE_OPEN) {
                            return;
                        }

                        if (BHPTools.isChangeable(bhp)) {
                            EntityManager em = OPDE.createEM();
                            try {
                                em.getTransaction().begin();

                                em.lock(em.merge(resident), LockModeType.OPTIMISTIC);
                                BHP myBHP = em.merge(bhp);
                                em.lock(myBHP, LockModeType.OPTIMISTIC);
                                em.lock(myBHP.getPrescriptionSchedule(), LockModeType.OPTIMISTIC);
                                em.lock(myBHP.getPrescription(), LockModeType.OPTIMISTIC);

                                myBHP.setStatus(BHPTools.STATE_REFUSED);
                                myBHP.setUser(em.merge(OPDE.getLogin().getUser()));
                                myBHP.setIst(new Date());
                                myBHP.setiZeit(SYSCalendar.whatTimeIDIs(new Date()));
                                myBHP.setMDate(new Date());

                                mapBHP2Pane.put(myBHP, createCP4(myBHP));
                                int position = mapShift2BHP.get(myBHP.getShift()).indexOf(bhp);
                                mapShift2BHP.get(bhp.getShift()).remove(position);
                                mapShift2BHP.get(bhp.getShift()).add(position, myBHP);
                                if (myBHP.isOnDemand()) {
                                    Collections.sort(mapShift2BHP.get(myBHP.getShift()), BHPTools.getOnDemandComparator());
                                } else {
                                    Collections.sort(mapShift2BHP.get(myBHP.getShift()));
                                }

                                em.getTransaction().commit();
                                mapShift2Pane.put(myBHP.getShift(), createCP4(myBHP.getShift()));
                                buildPanel(false);
                            } catch (OptimisticLockException ole) {
                                OPDE.warn(ole);
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();
                                }
                                if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
                                    OPDE.getMainframe().emptyFrame();
                                    OPDE.getMainframe().afterLogin();
                                }
                                OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            } catch (Exception e) {
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();
                                }
                                OPDE.fatal(e);
                            } finally {
                                em.close();
                            }

                        } else {
                            OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString(internalClassID + ".notchangeable")));
                        }
                    }
                });
                btnRefuse.setEnabled(!bhp.isOnDemand() && bhp.isOpen());
                cptitle.getRight().add(btnRefuse);

                /***
                 *      _     _         ____       __                ____  _                       _
                 *     | |__ | |_ _ __ |  _ \ ___ / _|_   _ ___  ___|  _ \(_)___  ___ __ _ _ __ __| |
                 *     | '_ \| __| '_ \| |_) / _ \ |_| | | / __|/ _ \ | | | / __|/ __/ _` | '__/ _` |
                 *     | |_) | |_| | | |  _ <  __/  _| |_| \__ \  __/ |_| | \__ \ (_| (_| | | | (_| |
                 *     |_.__/ \__|_| |_|_| \_\___|_|  \__,_|___/\___|____/|_|___/\___\__,_|_|  \__,_|
                 *
                 */
                final JButton btnRefuseDiscard = new JButton(SYSConst.icon22deleteall);
                btnRefuseDiscard.setPressedIcon(SYSConst.icon22deleteallPressed);
                btnRefuseDiscard.setAlignmentX(Component.RIGHT_ALIGNMENT);
                btnRefuseDiscard.setContentAreaFilled(false);
                btnRefuseDiscard.setBorder(null);
                btnRefuseDiscard.setToolTipText(SYSTools.toHTMLForScreen(OPDE.lang.getString(internalClassID + ".btnRefuseDiscard.tooltip")));
                btnRefuseDiscard.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (bhp.getStatus() != BHPTools.STATE_OPEN) {
                            return;
                        }

                        if (BHPTools.isChangeable(bhp)) {
                            EntityManager em = OPDE.createEM();
                            try {
                                em.getTransaction().begin();

                                em.lock(em.merge(resident), LockModeType.OPTIMISTIC);
                                BHP myBHP = em.merge(bhp);
                                em.lock(myBHP, LockModeType.OPTIMISTIC);
                                em.lock(myBHP.getPrescriptionSchedule(), LockModeType.OPTIMISTIC);
                                em.lock(myBHP.getPrescription(), LockModeType.OPTIMISTIC);

                                myBHP.setStatus(BHPTools.STATE_REFUSED_DISCARDED);
                                myBHP.setUser(em.merge(OPDE.getLogin().getUser()));
                                myBHP.setIst(new Date());
                                myBHP.setiZeit(SYSCalendar.whatTimeIDIs(new Date()));
                                myBHP.setMDate(new Date());

                                if (myBHP.shouldBeCalculated()) {
                                    MedInventory inventory = TradeFormTools.getInventory4TradeForm(resident, myBHP.getTradeForm());
                                    if (inventory != null) {
                                        MedInventoryTools.withdraw(em, em.merge(inventory), myBHP.getDose(), myBHP);
                                    } else {
                                        OPDE.getDisplayManager().addSubMessage(new DisplayMessage(internalClassID + ".NoInventory"));
                                    }
                                }

                                mapBHP2Pane.put(myBHP, createCP4(myBHP));
                                int position = mapShift2BHP.get(myBHP.getShift()).indexOf(bhp);
                                mapShift2BHP.get(bhp.getShift()).remove(position);
                                mapShift2BHP.get(bhp.getShift()).add(position, myBHP);
                                if (myBHP.isOnDemand()) {
                                    Collections.sort(mapShift2BHP.get(myBHP.getShift()), BHPTools.getOnDemandComparator());
                                } else {
                                    Collections.sort(mapShift2BHP.get(myBHP.getShift()));
                                }

                                em.getTransaction().commit();
                                mapShift2Pane.put(myBHP.getShift(), createCP4(myBHP.getShift()));
                                buildPanel(false);
                            } catch (OptimisticLockException ole) {
                                OPDE.warn(ole);
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();
                                }
                                if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
                                    OPDE.getMainframe().emptyFrame();
                                    OPDE.getMainframe().afterLogin();
                                }
                                OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            } catch (Exception e) {
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();
                                }
                                OPDE.fatal(e);
                            } finally {
                                em.close();
                            }

                        } else {
                            OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString(internalClassID + ".notchangeable")));
                        }
                    }
                });

                btnRefuseDiscard.setEnabled(!bhp.isOnDemand() && bhp.hasMed() && bhp.shouldBeCalculated() && bhp.isOpen());
                cptitle.getRight().add(btnRefuseDiscard);


                /***
                 *      _     _         _____                 _
                 *     | |__ | |_ _ __ | ____|_ __ ___  _ __ | |_ _   _
                 *     | '_ \| __| '_ \|  _| | '_ ` _ \| '_ \| __| | | |
                 *     | |_) | |_| | | | |___| | | | | | |_) | |_| |_| |
                 *     |_.__/ \__|_| |_|_____|_| |_| |_| .__/ \__|\__, |
                 *                                     |_|        |___/
                 */
                final JButton btnEmpty = new JButton(SYSConst.icon22empty);
                btnEmpty.setPressedIcon(SYSConst.icon22emptyPressed);
                btnEmpty.setAlignmentX(Component.RIGHT_ALIGNMENT);
                btnEmpty.setContentAreaFilled(false);
                btnEmpty.setBorder(null);
                btnEmpty.setToolTipText(OPDE.lang.getString(internalClassID + ".btnEmpty.tooltip"));
                btnEmpty.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (bhp.getStatus() == BHPTools.STATE_OPEN) {
                            return;
                        }

                        if (BHPTools.isChangeable(bhp)) {
                            EntityManager em = OPDE.createEM();
                            try {
                                em.getTransaction().begin();

                                em.lock(em.merge(resident), LockModeType.OPTIMISTIC);
                                BHP myBHP = em.merge(bhp);
                                em.lock(myBHP, LockModeType.OPTIMISTIC);
                                em.lock(myBHP.getPrescriptionSchedule(), LockModeType.OPTIMISTIC);
                                em.lock(myBHP.getPrescription(), LockModeType.OPTIMISTIC);

                                // the normal BHPs (those assigned to a NursingProcess) are reset to the OPEN state.
                                // TXs are deleted
                                myBHP.setStatus(BHPTools.STATE_OPEN);
                                myBHP.setUser(null);
                                myBHP.setIst(null);
                                myBHP.setiZeit(null);
                                myBHP.setMDate(new Date());

                                if (myBHP.shouldBeCalculated()) {
                                    for (MedStockTransaction tx : myBHP.getStockTransaction()) {
                                        em.remove(tx);
                                    }
                                    myBHP.getStockTransaction().clear();
                                }

                                mapBHP2Pane.put(myBHP, createCP4(myBHP));
                                int position = mapShift2BHP.get(myBHP.getShift()).indexOf(bhp);
                                mapShift2BHP.get(bhp.getShift()).remove(position);
                                mapShift2BHP.get(bhp.getShift()).add(position, myBHP);
                                if (myBHP.isOnDemand()) {
                                    Collections.sort(mapShift2BHP.get(myBHP.getShift()), BHPTools.getOnDemandComparator());
                                } else {
                                    Collections.sort(mapShift2BHP.get(myBHP.getShift()));
                                }

                                em.getTransaction().commit();
                                mapShift2Pane.put(myBHP.getShift(), createCP4(myBHP.getShift()));
                                buildPanel(false);
                            } catch (OptimisticLockException ole) {
                                OPDE.warn(ole);
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();
                                }
                                if (ole.getMessage().indexOf("Class> entity.info.Bewohner") > -1) {
                                    OPDE.getMainframe().emptyFrame();
                                    OPDE.getMainframe().afterLogin();
                                }
                                OPDE.getDisplayManager().addSubMessage(DisplayManager.getLockMessage());
                            } catch (Exception e) {
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();
                                }
                                OPDE.fatal(e);
                            } finally {
                                em.close();
                            }

                        } else {
                            OPDE.getDisplayManager().addSubMessage(new DisplayMessage(OPDE.lang.getString(internalClassID + ".notchangeable")));
                        }
                    }
                });
                btnEmpty.setEnabled(!bhp.isOpen());
                cptitle.getRight().add(btnEmpty);
            }


            /***
             *      _     _         ___        __
             *     | |__ | |_ _ __ |_ _|_ __  / _| ___
             *     | '_ \| __| '_ \ | || '_ \| |_ / _ \
             *     | |_) | |_| | | || || | | |  _| (_) |
             *     |_.__/ \__|_| |_|___|_| |_|_|  \___/
             *
             */
            final JButton btnInfo = new JButton(SYSConst.icon22info);

            btnInfo.setPressedIcon(SYSConst.icon22infoPressed);
            btnInfo.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btnInfo.setContentAreaFilled(false);
            btnInfo.setBorder(null);
            btnInfo.setToolTipText(OPDE.lang.getString(internalClassID + ".btnInfo.tooltip"));
            final JTextPane txt = new JTextPane();
            txt.setContentType("text/html");
            txt.setEditable(false);
            final JidePopup popupInfo = new JidePopup();
            popupInfo.setMovable(false);
            popupInfo.setContentPane(new JScrollPane(txt));
            popupInfo.removeExcludedComponent(txt);
            popupInfo.setDefaultFocusComponent(txt);


            btnInfo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    popupInfo.setOwner(btnInfo);
                    txt.setText(SYSTools.toHTML(SYSConst.html_div(bhp.getPrescription().getText())));
                    GUITools.showPopup(popupInfo, SwingConstants.SOUTH_WEST);
                }
            });

            btnInfo.setEnabled(!SYSTools.catchNull(bhp.getPrescription().getText()).isEmpty());
            cptitle.getRight().add(btnInfo);

        }

        bhpPane.setTitleLabelComponent(cptitle.getMain());
        bhpPane.setSlidingDirection(SwingConstants.SOUTH);

        final JTextPane contentPane = new JTextPane();
        contentPane.setEditable(false);
        contentPane.setContentType("text/html");
        bhpPane.setContentPane(contentPane);
        bhpPane.setBackground(bhp.getBG());
        bhpPane.setForeground(bhp.getFG());

        try {
            bhpPane.setCollapsed(true);
        } catch (PropertyVetoException e) {
            OPDE.error(e);
        }

        bhpPane.addCollapsiblePaneListener(new CollapsiblePaneAdapter() {
            @Override
            public void paneExpanded(CollapsiblePaneEvent collapsiblePaneEvent) {
                contentPane.setText(SYSTools.toHTML(PrescriptionTools.getPrescriptionAsHTML(bhp.getPrescription(), false, false, true, false)));
            }
        });

        bhpPane.setHorizontalAlignment(SwingConstants.LEADING);
        bhpPane.setOpaque(false);
        return bhpPane;
    }


    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the PrinterForm Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jspBHP = new JScrollPane();
        cpBHP = new CollapsiblePanes();

        //======== this ========
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //======== jspBHP ========
        {
            jspBHP.setBorder(null);

            //======== cpBHP ========
            {
                cpBHP.setLayout(new BoxLayout(cpBHP, BoxLayout.X_AXIS));
            }
            jspBHP.setViewportView(cpBHP);
        }
        add(jspBHP);
    }// </editor-fold>//GEN-END:initComponents


    private void prepareSearchArea() {
        searchPanes = new CollapsiblePanes();
        searchPanes.setLayout(new JideBoxLayout(searchPanes, JideBoxLayout.Y_AXIS));
        jspSearch.setViewportView(searchPanes);


        JPanel mypanel = new JPanel();
        mypanel.setLayout(new VerticalLayout());
        mypanel.setBackground(Color.WHITE);

        CollapsiblePane searchPane = new CollapsiblePane(OPDE.lang.getString(internalClassID));
        searchPane.setStyle(CollapsiblePane.PLAIN_STYLE);
        searchPane.setCollapsible(false);

        try {
            searchPane.setCollapsed(false);
        } catch (PropertyVetoException e) {
            OPDE.error(e);
        }

//        searchPanes.add(addCommands());
        GUITools.addAllComponents(searchPanes, addFilter());

        GUITools.addAllComponents(searchPanes, addKey());

        searchPanes.addExpansion();

    }

    private java.util.List<Component> addKey() {
        java.util.List<Component> list = new ArrayList<Component>();
        list.add(new JSeparator());
        list.add(new JLabel(OPDE.lang.getString("misc.msg.key")));
        list.add(new JLabel(OPDE.lang.getString("nursingrecords.bhp.keydescription3"), SYSConst.icon22stopSign, SwingConstants.LEADING));
        list.add(new JLabel(OPDE.lang.getString("nursingrecords.bhp.keydescription1"), SYSConst.icon22ledYellowOn, SwingConstants.LEADING));
        list.add(new JLabel(OPDE.lang.getString("nursingrecords.bhp.keydescription2"), SYSConst.icon22ledRedOn, SwingConstants.LEADING));
        list.add(new JLabel(OPDE.lang.getString("nursingrecords.bhp.keydescription4"), SYSConst.icon22ledOrangeOn, SwingConstants.LEADING));
        return list;
    }

    private java.util.List<Component> addFilter() {
        java.util.List<Component> list = new ArrayList<Component>();

        jdcDatum = new JDateChooser(new Date());
        jdcDatum.setFont(new Font("Arial", Font.PLAIN, 18));
        jdcDatum.setMinSelectableDate(BHPTools.getMinDatum(resident));

        jdcDatum.setBackground(Color.WHITE);
        jdcDatum.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (initPhase) {
                    return;
                }
                if (evt.getPropertyName().equals("date")) {
                    reloadDisplay();
                }
            }
        });
        list.add(jdcDatum);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setLayout(new HorizontalLayout(5));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JButton homeButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_start.png")));
        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jdcDatum.setDate(jdcDatum.getMinSelectableDate());
            }
        });
        homeButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_start_pressed.png")));
        homeButton.setBorder(null);
        homeButton.setBorderPainted(false);
        homeButton.setOpaque(false);
        homeButton.setContentAreaFilled(false);
        homeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton backButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_back.png")));
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DateMidnight current = new DateMidnight(jdcDatum.getDate());
                DateMidnight min = new DateMidnight(jdcDatum.getMinSelectableDate());
                if (current.equals(min)) {
                    return;
                }
                jdcDatum.setDate(SYSCalendar.addDate(jdcDatum.getDate(), -1));
            }
        });
        backButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_back_pressed.png")));
        backButton.setBorder(null);
        backButton.setBorderPainted(false);
        backButton.setOpaque(false);
        backButton.setContentAreaFilled(false);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));


        JButton fwdButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_play.png")));
        fwdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DateMidnight current = new DateMidnight(jdcDatum.getDate());
                if (current.equals(new DateMidnight())) {
                    return;
                }
                jdcDatum.setDate(SYSCalendar.addDate(jdcDatum.getDate(), 1));
            }
        });
        fwdButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_play_pressed.png")));
        fwdButton.setBorder(null);
        fwdButton.setBorderPainted(false);
        fwdButton.setOpaque(false);
        fwdButton.setContentAreaFilled(false);
        fwdButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton endButton = new JButton(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_end.png")));
        endButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jdcDatum.setDate(new Date());
            }
        });
        endButton.setPressedIcon(new ImageIcon(getClass().getResource("/artwork/32x32/bw/player_end_pressed.png")));
        endButton.setBorder(null);
        endButton.setBorderPainted(false);
        endButton.setOpaque(false);
        endButton.setContentAreaFilled(false);
        endButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));


        buttonPanel.add(homeButton);
        buttonPanel.add(backButton);
        buttonPanel.add(fwdButton);
        buttonPanel.add(endButton);

        list.add(buttonPanel);

//        panelFilter.setContentPane(labelPanel);

        return list;
    }

//


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JScrollPane jspBHP;
    private CollapsiblePanes cpBHP;
    // End of variables declaration//GEN-END:variables
}
