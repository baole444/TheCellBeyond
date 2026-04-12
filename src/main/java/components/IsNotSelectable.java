package components;

/**
 * Disable the Editor's ability to select an object that has this component.
 */
public class IsNotSelectable extends Component {
    /**
     * Create a new {@link IsNotSelectable} component.
     */
    public IsNotSelectable() {
        String name = IsNotSelectable.class.getSimpleName();
        super(name);
    }
}
