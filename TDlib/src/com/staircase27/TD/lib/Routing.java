/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.staircase27.TD.lib;

import com.staircase27.TD.lib.lib.Updatable.MapUpdate;
import com.staircase27.TD.lib.grid.Grid;
import com.staircase27.TD.lib.lib.TwoItems;
import com.staircase27.TD.lib.lib.Updatable.SetUpdate;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Simon Armstrong
 */
public class Routing {

    /**
     * creates a new routing object
     * @param grid
     * @param blockedPoints
     * @param ends
     */
    public Routing(Grid grid, HashSet<Point> blockedPoints, Set<Point> ends) {
        this.grid = grid;
        this.blockedPoints = blockedPoints;
        routes = new HashMap<Point, HashMap<Point, Integer>>();
        rhss = new HashMap<Point, HashMap<Point, Integer>>();
        for (Point end : ends) {
            routes.put(end, new HashMap<Point, Integer>());
            rhss.put(end, new HashMap<Point, Integer>());
        }
    }
    /**
     * 
     */
    public final Grid grid;
    HashSet<Point> blockedPoints;
    HashMap<Point, HashMap<Point, Integer>> rhss;
    HashMap<Point, HashMap<Point, Integer>> routes;

    /**
     * 
     */
    public void generateRoutes() {
        for (Point end : routes.keySet()) {
            generateRoute(end, routes.get(end), rhss.get(end));
        }
    }

    /**
     * Calculate the new routes and return changes to be either accepted or rejected.
     * NOTE: The returned map updates are all linked together!
     * @param blocked
     * @return A Map of the MapUpdate Objects for the updated routes. 
     */
    public Map<Point, MapUpdate<Point, Integer>> updateRoutesBlocked(Point blocked) {
        SetUpdate<Point> blockedPointsUpdate = new SetUpdate(blockedPoints);
        Set<Point> newBlockedPoints = blockedPointsUpdate.getSet();
        newBlockedPoints.add(blocked);
        Map<Point, MapUpdate<Point, Integer>> updates = new HashMap<Point, MapUpdate<Point, Integer>>();
        for (Point end : routes.keySet()) {
            updates.put(end, updateRouteBlocked(blocked, newBlockedPoints, end, routes.get(end), rhss.get(end)));
            blockedPointsUpdate.link(updates.get(end));
        }
        return updates;
    }

    /**
     * 
     * @param unblocked
     */
    public void updateRoutesUnblocked(Point unblocked) {
        blockedPoints.remove(unblocked);
        for (Point end : routes.keySet()) {
            updateRouteUnblocked(unblocked, end, routes.get(end), rhss.get(end));
        }
    }
    private Random rand = new Random();

    /**
     * 
     * @param target
     * @param current
     * @param previous
     * @return
     */
    public Point getNextPoint(Point target, Point current, Point previous) {
        List<TwoItems<Double, Point>> nexts = new LinkedList<TwoItems<Double, Point>>();
        HashMap<Point, Integer> route = routes.get(target);
        Integer distance = route.get(current);
        double total = 0;
        for (Point n : grid.getNeighbours(current)) {
            if (route.containsKey(n) && route.get(n) == distance - 1) {
                if (previous == null) {
                    total += 1;
                } else {
                    total += 2 + grid.getStrightness(previous, current, n);
                }
                nexts.add(new TwoItems<Double, Point>(total, n));
            }
        }
        double choice = rand.nextDouble() * total;
        Iterator<TwoItems<Double, Point>> it = nexts.iterator();
        while (it.hasNext()) {
            TwoItems<Double, Point> next = it.next();
            if (next.getA() > choice) {
                return next.getB();
            }
        }
        return null;
    }
    boolean DEBUG = true;
    String filebase = "out_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS").format(new Date());
    int index = 0;

    /**
     * outputs the data from the route planning to a file as:
     * x y \t type \t distance \t rhs
     * where type is 0 if blocked, 2 if on the U list and 1 otherwise or the above +3 if hilighted.
     * @param hilight
     * @param route
     * @param rhs
     * @param U
     */
    void printDebug(char key, Point hilight, Map<Point, Integer> route, Map<Point, Integer> rhs, Map<Point, Integer> U) {
        printDebug(key, Collections.singleton(hilight), route, rhs, U);
    }

    void printDebug(char key, Set<Point> hilight, Map<Point, Integer> route, Map<Point, Integer> rhs, Map<Point, Integer> U) {
        if (!DEBUG) {
            return;
        }
        try {
            BufferedWriter out;
            out = new BufferedWriter(new FileWriter(filebase + "_" + index + ".tdd"));
            out.write(key);
            out.write("\n");
            for (Point point : grid) {
                if (!grid.isValid(point)) {
                    System.out.println("YARG");
                }
                out.write(point.x + " " + point.y + "\t");
                int type = 0;
                if (blockedPoints.contains(point)) {
                    type += 1;
                }
                if (U.containsKey(point)) {
                    type += 2;
                }
                if (hilight.contains(point)) {
                    type += 4;
                }
                out.write("" + type + " ");
                int distance = Integer.MAX_VALUE;
                if (route.containsKey(point)) {
                    distance = route.get(point);
                }
                out.write(distance + "\t");
                distance = Integer.MAX_VALUE;
                if (rhs.containsKey(point)) {
                    distance = rhs.get(point);
                }
                out.write(distance + "\t");
                out.write("\n");
            }
            out.flush();
            out.close();
        } catch (IOException ex) {
            System.out.println("Oh dear");
        }
        index++;
    }
    /**
     * 
     */
    public static final Comparator<TwoItems<Integer, Point>> comparator = new Comparator<TwoItems<Integer, Point>>() {

        @Override
        public int compare(TwoItems<Integer, Point> o1, TwoItems<Integer, Point> o2) {
            int value = o1.getA().compareTo(o2.getA());
            if (value == 0) {
                value = o1.getB().x - o2.getB().x;
            }
            if (value == 0) {
                value = o1.getB().y - o2.getB().y;
            }
            return value;
        }
    };
    private TreeSet<TwoItems<Integer, Point>> Ul = new TreeSet<TwoItems<Integer, Point>>(comparator);
    private HashMap<Point, Integer> Um = new HashMap<Point, Integer>();

    private void UpdateVertex(Map<Point, Integer> route, Map<Point, Integer> rhs, TreeSet<TwoItems<Integer, Point>> Ul, Map<Point, Integer> Um, Point p) {
        if (route.containsKey(p)) {
            if ((!rhs.containsKey(p)) || route.get(p) != rhs.get(p)) {
                int k = route.get(p);
                if (rhs.containsKey(p) && rhs.get(p) < k) {
                    k = rhs.get(p);
                }
                Ul.add(new TwoItems<Integer, Point>(k, p));
                Um.put(p, k);
            } else {
                Um.remove(p);
            }
        } else {
            if (rhs.containsKey(p)) {
                int k = rhs.get(p);
                Ul.add(new TwoItems<Integer, Point>(k, p));
                Um.put(p, k);
            } else {
                Um.remove(p);
            }
        }
    }

    private void ComputePath(Point p_end, Set<Point> blockedPoints, Map<Point, Integer> route, Map<Point, Integer> rhs, TreeSet<TwoItems<Integer, Point>> Ul, Map<Point, Integer> Um) {
        while (!Ul.isEmpty()) {
            TwoItems<Integer, Point> entry = Ul.pollFirst();
            if (!(Um.containsKey(entry.getB()) && Um.get(entry.getB()) == entry.getA())) {
                continue;
            }
            Point p = entry.getB();
            int k_old = entry.getA();
            Um.remove(p);

            int k_new = Integer.MAX_VALUE;
            if (route.containsKey(p)) {
                k_new = route.get(p);
            }
            if (rhs.containsKey(p) && rhs.get(p) < k_new) {
                k_new = rhs.get(p);
            }
            if (k_old < k_new) {
                Ul.add(new TwoItems<Integer, Point>(k_new, p));
                Um.put(p, k_new);
            } else if (((!route.containsKey(p)) && rhs.containsKey(p)) || (route.containsKey(p) && rhs.containsKey(p) && route.get(p) > rhs.get(p))) {
                route.put(p, rhs.get(p));
                for (Point n : grid.getNeighbours(p)) {
                    if (n != p_end && !blockedPoints.contains(n)) {
                        if (!(rhs.containsKey(n) && rhs.get(n) < 1 + route.get(p))) {
                            rhs.put(n, 1 + route.get(p));
                        }
                    }
                    UpdateVertex(route, rhs, Ul, Um, n);
                }
            } else {
                int g_old = Integer.MAX_VALUE;
                if (route.containsKey(p)) {
                    g_old = route.get(p);
                }
                route.remove(p);
                for (Point n : grid.getNeighbours(p)) {
                    if (blockedPoints.contains(n)) {
                        rhs.remove(n);
                    } else {
                        if ((g_old == Integer.MAX_VALUE && !rhs.containsKey(n)) || g_old != Integer.MAX_VALUE && rhs.containsKey(n) && rhs.get(n) == 1 + g_old) {
                            int new_rhs = Integer.MAX_VALUE;
                            for (Point nn : grid.getNeighbours(n)) {
                                if (route.containsKey(nn) && 1 + route.get(nn) < new_rhs) {
                                    new_rhs = 1 + route.get(nn);
                                }
                            }
                            if (new_rhs == Integer.MAX_VALUE) {
                                rhs.remove(n);
                            } else {
                                rhs.put(n, new_rhs);
                            }
                        }
                    }
                    UpdateVertex(route, rhs, Ul, Um, n);
                }
                int new_rhs = Integer.MAX_VALUE;
                for (Point nn : grid.getNeighbours(p)) {
                    if (route.containsKey(nn) && !blockedPoints.contains(p) && 1 + route.get(nn) < new_rhs) {
                        new_rhs = 1 + route.get(nn);
                    }
                }
                if (new_rhs == Integer.MAX_VALUE) {
                    rhs.remove(p);
                } else {
                    rhs.put(p, new_rhs);
                }
                UpdateVertex(route, rhs, Ul, Um, p);
            }
            printDebug('0', p, route, rhs, Um);
        }
    }

    void generateRoute(Point p_end, Map<Point, Integer> route, Map<Point, Integer> rhs) {
        //the g and rhs values of all the points in the grid. not present is infinity.
        rhs.put(p_end, 0);
        Ul.add(new TwoItems<Integer, Point>(0, p_end));
        Um.put(p_end, 0);
        printDebug('0', p_end, route, rhs, Um);
        ComputePath(p_end, blockedPoints, route, rhs, Ul, Um);
    }

    MapUpdate<Point, Integer> updateRouteBlocked(Point blockedPoint, Set<Point> newBlockedPoints, Point p_end, Map<Point, Integer> route, Map<Point, Integer> rhs) {
        //records the updates to the route so can choose to accept or reject
        MapUpdate<Point, Integer> routeUpdate = new MapUpdate<Point, Integer>(route);
        Map<Point, Integer> newRoute = routeUpdate.getMap();
        MapUpdate<Point, Integer> rhsUpdate = new MapUpdate<Point, Integer>(rhs, routeUpdate);
        Map<Point, Integer> newRhs = rhsUpdate.getMap();
        newRhs.remove(blockedPoint);
        UpdateVertex(newRoute, newRhs, Ul, Um, blockedPoint);
        printDebug('0', blockedPoint, newRoute, newRhs, Um);
        ComputePath(p_end, newBlockedPoints, newRoute, newRhs, Ul, Um);
        return routeUpdate;
    }

    void updateRouteUnblocked(Point unblockedPoint, Point p_end, Map<Point, Integer> route, Map<Point, Integer> rhs) {
        int new_rhs = Integer.MAX_VALUE;
        for (Point n : grid.getNeighbours(unblockedPoint)) {
            if (route.containsKey(n) && new_rhs > 1 + route.get(n)) {
                new_rhs = 1 + route.get(n);
            }
        }
        if (new_rhs == Integer.MAX_VALUE) {
            rhs.remove(unblockedPoint);
        } else {
            rhs.put(unblockedPoint, new_rhs);
        }
        UpdateVertex(route, rhs, Ul, Um, unblockedPoint);
        printDebug('0', unblockedPoint, route, rhs, Um);
        ComputePath(p_end, blockedPoints, route, rhs, Ul, Um);
    }
}
