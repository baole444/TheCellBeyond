package editor.project;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectPrefabricationMap {
    private String sheet;
    private Map<String, AnimationMap> animations;
    private Map<String, ConditionMap> conditions;

    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public Map<String, AnimationMap> getAnimations() {
        return animations;
    }

    public void setAnimations(Map<String, AnimationMap> animations) {
        this.animations = animations;
    }

    public Map<String, ConditionMap> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, ConditionMap> conditions) {
        this.conditions = conditions;
    }

    @Override
    public String toString() {
        return "Prefab{" +
                "sheet = '" + sheet + '\'' +
                ", animations = " + animations +
                ", conditions = " + conditions +
                '}';
    }

    public void validate() {
        if (animations == null || conditions == null) return;

        Set<String> validStates = animations.keySet();

        for (Map.Entry<String, ConditionMap> entry : conditions.entrySet()) {
            ConditionMap condition = entry.getValue();
            String conditionName = entry.getKey();

            for (String fromState : condition.getFrom()) {
                if (!validStates.contains(fromState)) {
                    throw new IllegalArgumentException("Invalid 'from' state: '"
                            + fromState + "' in '" + conditionName
                            + "' condition. No such state declared under Animation."
                    );
                }
            }

            if (!validStates.contains(condition.getTo())) {
                throw new IllegalArgumentException("Invalid 'to' state: '"
                        + condition.getTo() + "' in '" + conditionName
                        + "' condition. No such state declared under Animation."
                );
            }
        }

    }

    public static class AnimationMap {
        private final String name;
        private boolean loop;
        private List<Integer> sequence;

        public AnimationMap(String name, boolean loop, List<Integer> sequence) {
            this.name = name;
            this.loop = loop;
            this.sequence = sequence;
        }

        public String getName() {
            return name;
        }

        public boolean isLoop() {
            return loop;
        }

        public void setLoop(boolean loop) {
            this.loop = loop;
        }

        public List<Integer> getSequence() {
            return sequence;
        }

        public void setSequence(List<Integer> sequence) {
            this.sequence = sequence;
        }

         @Override
        public String toString() {
            return "{name = '" + name + "', loop = " + loop + ", sequence = " + sequence + "}";
        }
    }

    public static class ConditionMap {
        private final String name;
        private List<String> from;
        private String to;

        public ConditionMap(String name, List<String> from, String to) {
            this.name = name;
            this.from = from;
            this.to = to;
        }

        public String getName() {
            return name;
        }

        public List<String> getFrom() {
            return from;
        }

        public void setFrom(List<String> from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }
    }
}
