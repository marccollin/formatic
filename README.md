# Formatic

[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Generate responsive Thymeleaf forms from Java annotations—with Spring Boot and your choice of CSS framework.**

![Example Formatic form](formatic_form.jpg)

## ✨ Features

- Generate form fields from annotations on Java DTOs.
- Support text, date, number, choice, checkbox, file, and other common input types.
- Integrate with Spring Boot, Spring MVC, and Thymeleaf.
- Choose bundled Bootstrap, Tailwind CSS, or Bulma templates.
- Customize field metadata and override Thymeleaf templates.

## 🚀 Quick Start

Formatic requires Java 21 or higher. Clone the repository and start the example app:

```bash
git clone https://github.com/marccollin/formatic.git
cd formatic
./gradlew :formatic-example:bootRun
```

Open the example form:

```text
http://localhost:8080/form?class=com.formatic.example.dto.Editor&cssLibrary=BOOTSTRAP
```

The [Running the Example](#-running-the-example) section has more sample URLs.

## 📦 Installation

Formatic artifacts are not currently published to Maven Central or another Maven repository. Until they are published, use the Gradle project dependency or composite build.

For applications in this repository's multi-project build:

```groovy
dependencies {
    implementation project(':formatic-spring')
}
```

For a separate Gradle project, add the Formatic checkout as a composite build in `settings.gradle` (adjust the path as needed):

```groovy
includeBuild('../formatic')
```

Then add the Spring module to `build.gradle`:

```groovy
dependencies {
    implementation 'com.formatic:formatic-spring:1.0.0'
}
```

**Maven:** Maven dependency coordinates are not available until Formatic publishes artifacts. The Spring module includes the core library and Thymeleaf integration.

## 📖 Basic Usage

Annotate fields in a DTO to describe the form:

```java
import com.formatic.core.annotation.DateInput;
import com.formatic.core.annotation.EmailInput;
import com.formatic.core.annotation.TextInput;
import java.time.LocalDate;

public class ContactForm {
    @TextInput(label = "Name", required = true, maxLength = 80)
    private String name;

    @EmailInput(required = true)
    private String email;

    @DateInput
    private LocalDate preferredDate;
}
```

Formatic's Spring integration builds field metadata from the annotations and renders it with Thymeleaf templates. The example app demonstrates a complete controller and view; see the [User Guide](docs/USER_GUIDE.md) for field options and application integration details.

## 🎨 CSS Framework Support

| Framework | Layouts |
|---|---|
| Bootstrap | Vertical and horizontal |
| Tailwind CSS | Vertical |
| Bulma | Vertical and responsive two-column horizontal |

The bundled templates render the form; include the corresponding CSS framework in your application. See [CSS and layout options](docs/USER_GUIDE.md#testing-the-example-application).

## 🔧 Advanced Topics

The [User Guide](docs/USER_GUIDE.md) covers field constraints and validation, metadata builder modes, hybrid forms, accessibility, file uploads, dynamic options, template overrides, and example endpoints.

## 🧪 Running the Example

Start the demo application from the repository root:

```bash
./gradlew :formatic-example:bootRun
```

Examples:

```text
# Bootstrap vertical (default)
http://localhost:8080/form?class=com.formatic.example.dto.Editor&cssLibrary=BOOTSTRAP

# Bulma responsive two-column
http://localhost:8080/form?class=com.formatic.example.dto.Editor&cssLibrary=BULMA&form=HORIZONTAL

# Tailwind vertical
http://localhost:8080/form?class=com.formatic.example.dto.Editor&cssLibrary=TAILWIND
```

See the [example application guide](docs/USER_GUIDE.md#testing-the-example-application) for builder modes, prefilled forms, and hybrid forms.

## 🤝 Contributing

Issues and pull requests are welcome. See [Contributing](docs/USER_GUIDE.md#contributing) for contribution guidance.

## 📄 License

Formatic is licensed under the [Apache License 2.0](LICENSE).
