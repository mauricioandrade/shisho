package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.classes;
import static org.assertj.core.api.Assertions.assertThat;

class JavaxProjectsTest {

    @Test
    void aProjectImportingTheRenamedPackagesIsJavax() {
        assertThat(JavaxProjects.isJavax(classes("import javax.validation.constraints.*; class A {}"))).isTrue();
        assertThat(JavaxProjects.valid(classes("import javax.servlet.http.HttpServletRequest; class A {}")))
                .isEqualTo("javax.validation.Valid");
    }

    @Test
    void anyJakartaImportMakesItJakarta() {
        assertThat(JavaxProjects.isJavax(classes("""
                import javax.persistence.Entity;
                import jakarta.validation.Valid;
                class A {}
                """))).as("halfway through the migration, the new namespace is already there").isFalse();
    }

    @Test
    void javaxAnnotationAndOtherJdkPackagesDoNotCount() {
        assertThat(JavaxProjects.isJavax(classes("""
                import javax.annotation.Nullable;
                import javax.crypto.Cipher;
                class A {}
                """))).isFalse();
        assertThat(JavaxProjects.valid(classes("class A {}"))).isEqualTo("jakarta.validation.Valid");
    }
}
