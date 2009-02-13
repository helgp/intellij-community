package org.jetbrains.idea.maven;

import com.intellij.compiler.impl.TranslatingCompilerFilesMonitor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ResourceFilteringTest extends MavenImportingTestCase {
  public void testBasic() throws Exception {
    createProjectSubFile("resources/file.properties", "value=${project.version}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<build>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    assertSources("project", "resources");
    compileModules("project");

    assertResult("target/classes/file.properties", "value=1");
  }

  public void testPomArtifactId() throws Exception {
    createProjectSubFile("resources/file.properties", "value=${pom.artifactId}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<build>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    assertSources("project", "resources");
    compileModules("project");

    assertResult("target/classes/file.properties", "value=project");
  }

  public void testPomVersionInModules() throws Exception {
    createProjectSubFile("m1/resources/file.properties", "value=${pom.version}");

    createProjectPom("<groupId>test</groupId>" +
                     "<artifactId>project</artifactId>" +
                     "<version>1</version>" +

                     "<modules>" +
                     "  <module>m1</module>" +
                     "</modules>");

    createModulePom("m1",
                    "<groupId>test</groupId>" +
                    "<artifactId>m1</artifactId>" +
                    "<version>2</version>" +

                    "<build>" +
                    "  <resources>" +
                    "    <resource>" +
                    "      <directory>resources</directory>" +
                    "      <filtering>true</filtering>" +
                    "    </resource>" +
                    "  </resources>" +
                    "</build>");
    importProject();

    assertSources("m1", "resources");
    compileModules("project", "m1");

    assertResult("m1/target/classes/file.properties", "value=2");
  }

  public void testFilteringTestResources() throws Exception {
    createProjectSubFile("resources/file.properties", "value=${project.version}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<build>" +
                  "  <testResources>" +
                  "    <testResource>" +
                  "      <directory>resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </testResource>" +
                  "  </testResources>" +
                  "</build>");
    assertTestSources("project", "resources");
    compileModules("project");

    assertResult("target/test-classes/file.properties", "value=1");
  }

  public void testFilterWithSeveralResourceFolders() throws Exception {
    createProjectSubFile("resources1/file1.properties", "value=${project.version}");
    createProjectSubFile("resources2/file2.properties", "value=${project.version}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<build>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources1</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "    <resource>" +
                  "      <directory>resources2</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    assertSources("project", "resources1", "resources2");
    compileModules("project");

    assertResult("target/classes/file1.properties", "value=1");
    assertResult("target/classes/file2.properties", "value=1");
  }

  public void testFilterWithSeveralModules() throws Exception {
    createProjectSubFile("module1/resources/file1.properties", "value=${project.version}");
    createProjectSubFile("module2/resources/file2.properties", "value=${project.version}");

    VirtualFile m1 = createModulePom("module1",
                                     "<groupId>test</groupId>" +
                                     "<artifactId>module1</artifactId>" +
                                     "<version>1</version>" +

                                     "<build>" +
                                     "  <resources>" +
                                     "    <resource>" +
                                     "      <directory>resources</directory>" +
                                     "      <filtering>true</filtering>" +
                                     "    </resource>" +
                                     "  </resources>" +
                                     "</build>");

    VirtualFile m2 = createModulePom("module2",
                                     "<groupId>test</groupId>" +
                                     "<artifactId>module2</artifactId>" +
                                     "<version>2</version>" +

                                     "<build>" +
                                     "  <resources>" +
                                     "    <resource>" +
                                     "      <directory>resources</directory>" +
                                     "      <filtering>true</filtering>" +
                                     "    </resource>" +
                                     "  </resources>" +
                                     "</build>");

    importSeveralProjects(m1, m2);
    assertSources("module1", "resources");
    assertSources("module2", "resources");
    compileModules("module1", "module2");

    assertResult(m1, "target/classes/file1.properties", "value=1");
    assertResult(m2, "target/classes/file2.properties", "value=2");
  }

  public void testDoNotFilterIfNotRequested() throws Exception {
    createProjectSubFile("resources1/file1.properties", "value=${project.version}");
    createProjectSubFile("resources2/file2.properties", "value=${project.version}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<build>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources1</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "    <resource>" +
                  "      <directory>resources2</directory>" +
                  "      <filtering>false</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    assertSources("project", "resources1", "resources2");
    compileModules("project");

    assertResult("target/classes/file1.properties", "value=1");
    assertResult("target/classes/file2.properties", "value=${project.version}");
  }

  public void testDoNotChangeFileIfPropertyIsNotResolved() throws Exception {
    createProjectSubFile("resources/file.properties", "value=${foo.bar}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<build>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    assertSources("project", "resources");
    compileModules("project");

    assertResult("target/classes/file.properties", "value=${foo.bar}");
  }

  public void testChangingResolvedPropsBackWhenSettingsIsChange() throws Exception {
    createProjectSubFile("resources/file.properties", "value=${project.version}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<build>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    compileModules("project");
    assertResult("target/classes/file.properties", "value=1");

    updateProjectPom("<groupId>test</groupId>" +
                     "<artifactId>project</artifactId>" +
                     "<version>1</version>" +

                     "<build>" +
                     "  <resources>" +
                     "    <resource>" +
                     "      <directory>resources</directory>" +
                     "      <filtering>false</filtering>" +
                     "    </resource>" +
                     "  </resources>" +
                     "</build>");
    importProject();
    compileModules("project");

    assertResult("target/classes/file.properties", "value=${project.version}");
  }

  public void testSameFileInSourcesAndTestSources() throws Exception {
    createProjectSubFile("src/main/resources/file.properties", "foo=${foo.main}");
    createProjectSubFile("src/test/resources/file.properties", "foo=${foo.test}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<properties>" +
                  "  <foo.main>main</foo.main>" +
                  "  <foo.test>test</foo.test>" +
                  "</properties>" +

                  "<build>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>src/main/resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "  <testResources>" +
                  "    <testResource>" +
                  "      <directory>src/test/resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </testResource>" +
                  "  </testResources>" +
                  "</build>");
    compileModules("project");

    assertResult("target/classes/file.properties", "foo=main");
    assertResult("target/test-classes/file.properties", "foo=test");
  }

  public void testCustomFilters() throws Exception {
    createProjectSubFile("filters/filter1.properties",
                         "xxx=value\n" +
                         "yyy=${project.version}\n");
    createProjectSubFile("filters/filter2.properties", "zzz=value2");
    createProjectSubFile("resources/file.properties",
                         "value1=${xxx}\n" +
                         "value2=${yyy}\n" +
                         "value3=${zzz}\n");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<build>" +
                  "  <filters>" +
                  "    <filter>filters/filter1.properties</filter>" +
                  "    <filter>filters/filter2.properties</filter>" +
                  "  </filters>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    assertSources("project", "resources");
    compileModules("project");

    assertResult("target/classes/file.properties", "value1=value\n" +
                                                   "value2=1\n" +
                                                   "value3=value2\n");
  }

  public void testCustomFilterWithPropertyInThePath() throws Exception {
    createProjectSubFile("filters/filter.properties", "xxx=value");
    createProjectSubFile("resources/file.properties", "value=${xxx}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<properties>" +
                  " <some.path>" + getProjectPath() + "/filters</some.path>" +
                  "</properties>" +

                  "<build>" +
                  "  <filters>" +
                  "    <filter>${some.path}/filter.properties</filter>" +
                  "  </filters>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    assertSources("project", "resources");
    compileModules("project");

    assertResult("target/classes/file.properties", "value=value");
  }

  public void testCustomFiltersFromProfiles() throws Exception {
    createProjectSubFile("filters/filter1.properties", "xxx=value1");
    createProjectSubFile("filters/filter2.properties", "yyy=value2");
    createProjectSubFile("resources/file.properties",
                         "value1=${xxx}\n" +
                         "value2=${yyy}\n");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +

                  "<profiles>" +
                  "  <profile>" +
                  "    <id>one</id>" +
                  "    <build>" +
                  "      <filters>" +
                  "        <filter>filters/filter1.properties</filter>" +
                  "      </filters>" +
                  "    </build>" +
                  "  </profile>" +
                  "  <profile>" +
                  "    <id>two</id>" +
                  "    <activation>" +
                  "      <activeByDefault>true</activeByDefault>" +
                  "    </activation>" +
                  "    <build>" +
                  "      <filters>" +
                  "        <filter>filters/filter2.properties</filter>" +
                  "      </filters>" +
                  "    </build>" +
                  "  </profile>" +
                  "</profiles>" +

                  "<build>" +
                  "  <resources>" +
                  "    <resource>" +
                  "      <directory>resources</directory>" +
                  "      <filtering>true</filtering>" +
                  "    </resource>" +
                  "  </resources>" +
                  "</build>");
    assertSources("project", "resources");
    compileModules("project");
    assertResult("target/classes/file.properties", "value1=${xxx}\n" +
                                                   "value2=value2\n");

    importProjectWithProfiles("one");
    compileModules("project");
    assertResult("target/classes/file.properties", "value1=value1\n" +
                                                   "value2=value2\n");
  }

  public void testPluginDirectoriesFiltering() throws Exception {
    if (ignore()) return;

    createProjectSubFile("filters/filter.properties", "xxx=value");
    createProjectSubFile("webdir1/file1.properties", "value=${xxx}");
    createProjectSubFile("webdir2/file2.properties", "value=${xxx}");

    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>" +
                  "<packaging>war</packaging>" +

                  "<build>" +
                  "  <filters>" +
                  "    <filter>filters/filter.properties</filter>" +
                  "  </filters>" +
                  "  <plugins>" +
                  "    <plugin>" +
                  "      <artifactId>maven-war-plugin</artifactId>\n" +
                  "      <configuration>" +
                  "        <webResources>" +
                  "          <resource>" +
                  "            <directory>webdir1</directory>" +
                  "            <filtering>true</filtering>" +
                  "          </resource>" +
                  "          <resource>" +
                  "            <directory>webdir2</directory>" +
                  "            <filtering>false</filtering>" +
                  "          </resource>" +
                  "        </webResources>" +
                  "      </configuration>" +
                  "    </plugin>" +
                  "  </plugins>" +
                  "</build>");

    assertSources("project", "webdir");
    compileModules("project");
    assertResult("target/classes/file1.properties", "value=value");
    assertResult("target/classes/file2.properties", "value=${xxx}");
  }

  private void assertResult(String relativePath, String content) throws IOException {
    assertResult(myProjectPom, relativePath, content);
  }

  private void assertResult(VirtualFile pomFile, String relativePath, String content) throws IOException {
    VirtualFile file = pomFile.getParent().findFileByRelativePath(relativePath);
    assertNotNull(file);
    assertEquals(content, VfsUtil.loadText(file));
  }

  private void compileModules(String... modules) {
    for (String each : modules) {
      setupJdkForModule(each);
    }

    List<VirtualFile> roots = Arrays.asList(ProjectRootManager.getInstance(myProject).getContentSourceRoots());
    TranslatingCompilerFilesMonitor.getInstance().scanSourceContent(myProject, roots, roots.size(), true);

    CompilerManager.getInstance(myProject).make(new CompileStatusNotification() {
      public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
        assertFalse(aborted);
        assertEquals(0, errors);
        assertEquals(0, warnings);
      }
    });
  }
}
