package fixture;

import module java.base;

import org.springframework.beans.factory.annotation.Autowired;

public class Java25Fixture {

    @Autowired
    private Repo repo;

    private final int size;

    sealed interface Shape permits Circle, Square {}
    record Circle(double r) implements Shape {}
    record Square(double s) implements Shape {}
    interface Repo {}

    // JEP 513 - flexible constructor bodies (final in 25)
    public Java25Fixture(int size) {
        if (size < 0) throw new IllegalArgumentException("negative");
        this.size = size;
        super();
    }

    double area(Shape shape) {
        return switch (shape) {
            case Circle(double r) -> Math.PI * r * r;
            case Square(var _) -> 0; // JEP 456 - unnamed pattern
        };
    }

    int count(List<String> items) {
        int total = 0;
        for (String _ : items) {           // JEP 456 - unnamed variable
            total++;
        }
        try {
            Integer.parseInt("x");
        } catch (NumberFormatException _) { // unnamed in catch
            System.out.println("bad");
        }
        items.forEach(_ -> {});             // unnamed lambda param
        return total;
    }
}
