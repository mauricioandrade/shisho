package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SarifReportWriterTest {

    private static final Path WORKSPACE = Path.of("/repo").toAbsolutePath();
    private static final ArchitectureProfile LAYERED = new ArchitectureProfile("layered", "Camadas técnicas (Layered)",
            Family.SIMPLE, new Signature(List.of("controller", "service"), List.of(), 2), List.of(),
            new Cost(new ClassRange(4, 6), Complexity.LOW), List.of("crud_simples"), List.of(), List.of("service_deus"),
            List.of("package-by-feature"), Optional.empty(), List.of(), "");

    @Test
    void writesSarifForGitHubCodeScanning() {
        Analysis analysis = analysis(WORKSPACE.resolve("api"), List.of(
                new Lesson("missing-actuator", Severity.LEVE, Path.of("pom.xml"), 0, "kata.missing-actuator.lesson", List.of()),
                new Lesson("field-injection", Severity.GRAVE, Path.of("src/main/java/app/Usuario Controller.java"), 14,
                        "kata.field-injection.lesson", List.of("service")),
                new Lesson("field-injection", Severity.GRAVE, Path.of("src/main/java/app/B.java"), 3,
                        "kata.field-injection.lesson", List.of("clock"))));

        assertThat(render(analysis)).isEqualTo("""
                {
                  "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
                  "version": "2.1.0",
                  "runs": [
                    {
                      "tool": {
                        "driver": {
                          "name": "Shishō",
                          "informationUri": "https://github.com/mauricioandrade/shiso",
                          "version": "1.2.3",
                          "rules": [
                            {
                              "id": "missing-actuator",
                              "shortDescription": {
                                "text": "Aplicação web sem actuator"
                              },
                              "helpUri": "https://github.com/mauricioandrade/shiso#katas",
                              "defaultConfiguration": {
                                "level": "note"
                              }
                            },
                            {
                              "id": "field-injection",
                              "shortDescription": {
                                "text": "Injeção de dependência em campo"
                              },
                              "helpUri": "https://github.com/mauricioandrade/shiso#katas",
                              "defaultConfiguration": {
                                "level": "error"
                              }
                            }
                          ]
                        }
                      },
                      "invocations": [
                        {
                          "executionSuccessful": true,
                          "toolExecutionNotifications": [
                            {
                              "level": "warning",
                              "message": {
                                "text": "⚠ src/main/java/Quebrado.java:4 não pôde ser analisado: Parse error"
                              },
                              "locations": [
                                {
                                  "physicalLocation": {
                                    "artifactLocation": {
                                      "uri": "api/src/main/java/Quebrado.java",
                                      "uriBaseId": "%SRCROOT%"
                                    },
                                    "region": {
                                      "startLine": 4
                                    }
                                  }
                                }
                              ]
                            }
                          ]
                        }
                      ],
                      "results": [
                        {
                          "ruleId": "missing-actuator",
                          "level": "note",
                          "message": {
                            "text": "Aplicação web sem spring-boot-starter-actuator. Sem /actuator/health, quem orquestra a aplicação (Kubernetes, ECS, um load balancer) não sabe se ela está viva, e não há métricas. Adicione o starter e exponha só os endpoints necessários."
                          },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": {
                                  "uri": "api/pom.xml",
                                  "uriBaseId": "%SRCROOT%"
                                }
                              }
                            }
                          ],
                          "properties": {
                            "severity": "LEVE"
                          }
                        },
                        {
                          "ruleId": "field-injection",
                          "level": "error",
                          "message": {
                            "text": "Injeção em campo em service. Use construtor explícito com private final: a dependência fica visível e o objeto, imutável."
                          },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": {
                                  "uri": "api/src/main/java/app/Usuario%20Controller.java",
                                  "uriBaseId": "%SRCROOT%"
                                },
                                "region": {
                                  "startLine": 14
                                }
                              }
                            }
                          ],
                          "properties": {
                            "severity": "GRAVE"
                          }
                        },
                        {
                          "ruleId": "field-injection",
                          "level": "error",
                          "message": {
                            "text": "Injeção em campo em clock. Use construtor explícito com private final: a dependência fica visível e o objeto, imutável."
                          },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": {
                                  "uri": "api/src/main/java/app/B.java",
                                  "uriBaseId": "%SRCROOT%"
                                },
                                "region": {
                                  "startLine": 3
                                }
                              }
                            }
                          ],
                          "properties": {
                            "severity": "GRAVE"
                          }
                        }
                      ],
                      "properties": {
                        "project": "api",
                        "javaVersion": 21,
                        "score": 79,
                        "rank": "2º kyū",
                        "architecture": {
                          "id": "layered",
                          "name": "Camadas técnicas (Layered)",
                          "confidence": 100,
                          "signals": [
                            "controller",
                            "service"
                          ]
                        },
                        "verdict": {
                          "kind": "underengineering",
                          "text": "Veredito: subengenharia. O projeto já não cabe na arquitetura Camadas técnicas (Layered):",
                          "evidence": [
                            "PedidoService tem 31 métodos e 9 dependências no construtor."
                          ],
                          "next": "Próximo passo natural: package-by-feature."
                        }
                      }
                    }
                  ]
                }
                """);
    }

    @Test
    void usesPathsRelativeToTheProjectWhenItIsOutsideTheWorkingDirectory() {
        Analysis analysis = analysis(Path.of("/elsewhere/api").toAbsolutePath(), List.of(
                new Lesson("system-out", Severity.MEDIA, Path.of("src/main/java/A.java"), 2, "kata.system-out.lesson", List.of("System.out.println"))));

        assertThat(render(analysis)).contains("\"uri\": \"src/main/java/A.java\"").contains("\"level\": \"warning\"");
    }

    private static Analysis analysis(Path root, List<Lesson> lessons) {
        ArchitectureMatch match = new ArchitectureMatch(LAYERED, List.of("controller", "service"), 100, List.of(),
                new Verdict.Underengineering(List.of(new Evidence(
                        "service_deus", List.of("PedidoService", 31, 9))), List.of("package-by-feature")));
        int score = 100 - 10 - 10 - 1;
        return new Analysis(root, "api", new JavaVersion(21), true, Optional.of(match), lessons,
                List.of(new SourceFile.Unparseable(Path.of("src/main/java/Quebrado.java"), 4, "Parse error")), score, Rank.forScore(score));
    }

    private static String render(Analysis analysis) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new SarifReportWriter(new PrintStream(buffer, true, StandardCharsets.UTF_8), new Messages(Messages.DEFAULT_LOCALE), "1.2.3", WORKSPACE)
                .write(analysis);
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
