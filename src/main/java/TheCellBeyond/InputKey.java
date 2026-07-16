package TheCellBeyond;

import scripting.API;

/**
 * InputKey represent an input code value and its source.
 * @param type the type of the input
 * @param code the number constant for the input
 */
@API
public record InputKey(InputType type, int code) {}
