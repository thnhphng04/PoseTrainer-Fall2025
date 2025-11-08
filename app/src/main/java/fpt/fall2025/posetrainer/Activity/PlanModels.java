package fpt.fall2025.posetrainer.Activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanModels {
    public static class Plan {
        public int weekCount;
        public List<Day> days = new ArrayList<>();
        
        public static Plan from(Map map) {
            Plan p = new Plan();
            if (map == null) {
                return p;
            }
            
            try {
                Object wc = map.get("weekCount");
                if (wc instanceof Number) {
                    p.weekCount = ((Number) wc).intValue();
                }
                
                Object dayListObj = map.get("days");
                if (dayListObj instanceof List) {
                    List dayList = (List) dayListObj;
                    for (Object o : dayList) {
                        if (o instanceof Map) {
                            Day day = Day.from((Map) o);
                            if (day != null) {
                                p.days.add(day);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Log error but return partial plan
                android.util.Log.e("PlanModels", "Error parsing Plan", e);
            }
            
            return p;
        }
    }
    
    public static class Day {
        public int dayIndex;
        public String focus;
        public int estMinutes;
        public List<Item> items = new ArrayList<>();
        
        public static Day from(Map map) {
            if (map == null) {
                return null;
            }
            
            Day d = new Day();
            try {
                Object di = map.get("dayIndex");
                if (di instanceof Number) {
                    d.dayIndex = ((Number) di).intValue();
                }
                
                Object em = map.get("estMinutes");
                if (em instanceof Number) {
                    d.estMinutes = ((Number) em).intValue();
                }
                
                Object focusObj = map.get("focus");
                if (focusObj != null) {
                    String focusStr = String.valueOf(focusObj);
                    d.focus = "null".equals(focusStr) ? null : focusStr;
                }
                
                Object itemListObj = map.get("items");
                if (itemListObj instanceof List) {
                    List itemList = (List) itemListObj;
                    for (Object o : itemList) {
                        if (o instanceof Map) {
                            Item item = Item.from((Map) o);
                            if (item != null) {
                                d.items.add(item);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("PlanModels", "Error parsing Day", e);
                return null;
            }
            
            return d;
        }
    }
    
    public static class Item {
        public String exerciseId;
        public String name;
        public int sets;
        public int reps;
        public int restSec;
        
        public static Item from(Map map) {
            if (map == null) {
                return null;
            }
            
            Item it = new Item();
            try {
                Object exerciseIdObj = map.get("exerciseId");
                if (exerciseIdObj != null) {
                    String exerciseIdStr = String.valueOf(exerciseIdObj);
                    it.exerciseId = "null".equals(exerciseIdStr) ? null : exerciseIdStr;
                }
                
                Object nameObj = map.get("name");
                if (nameObj != null) {
                    String nameStr = String.valueOf(nameObj);
                    it.name = "null".equals(nameStr) ? null : nameStr;
                }
                
                Object s = map.get("sets");
                if (s instanceof Number) {
                    it.sets = ((Number) s).intValue();
                }
                
                Object r = map.get("reps");
                if (r instanceof Number) {
                    it.reps = ((Number) r).intValue();
                }
                
                Object rest = map.get("restSec");
                if (rest instanceof Number) {
                    it.restSec = ((Number) rest).intValue();
                }
            } catch (Exception e) {
                android.util.Log.e("PlanModels", "Error parsing Item", e);
                return null;
            }
            
            return it;
        }
    }
}
