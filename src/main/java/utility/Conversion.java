package utility;

import java.util.UUID;

public class Conversion {
    public static int uuidToInt(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return 0;
        }

        try {
            UUID id = UUID.fromString(uuid);

            long mS = id.getMostSignificantBits();
            long lS = id.getLeastSignificantBits();

            int result = (int) (mS ^ lS);

            if (result == 0) {
                result = 1;
            }

            return result;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
}
