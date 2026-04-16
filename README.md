# Static Semantic Merge

## A tool for integrating static analysis into the merge process

The **Static Semantic Merge (SSM)** uses the analysis implementations from the **conflict-static-analysis** project to identify interferences in integration scenarios.
> The **conflict-static-analysis** is a project that implements algorithms to analyze Java projects, identifying potential conflicts during the integration process.

### Goal

The main goal of **SSM** is to be integrated into the software development process to detect semantic conflicts in merge scenarios. This is achieved through the use of **conflict-static-analysis** and the **Mining Framework**.

### Integration into the Merge Process

**SSM** can be coupled to the merge process using the post-merge hook. The idea is that **SSM** runs right after a textually conflict-free merge is completed.

**SSM** consists of four main actions:

1. Generate the build of the project being merged.
2. Collect the lines that were modified within the same method.
3. Execute the analyses.
4. Check if the analyses indicated a conflict and, if so, revert the commit.

### Hooks

Hooks are custom scripts that are triggered when certain actions occur.

The post-merge script is located in `.git/hooks` within the project and is executed by git right after a merge without textual conflicts. The script collects the necessary information and passes it to the **SSM** JAR as parameters.

Example post-merge script:

```bash
#!/bin/sh
head=$(git rev-parse HEAD)
parents=$(git log --pretty=%P -n 1 $head)
base=$(git merge-base $parents)
mergerPath="PATH_TO_SSM_FOLDER"
gradlePath="PATH_TO_GRADLE_BIN"
mavenPath="PATH_TO_MAVEN_BIN"
java -jar ${mergerPath}/static-semantic-merge-1.0-SNAPSHOT.jar $head $parents $base $mergerPath $gradlePath $mavenPath
```

### Building with Gradle

To build the project with Gradle, use the following command at the project root:

```bash
./gradlew build
```

### Project Structure

At the project root, you will find the example post-merge script, as well as the following folders:

StudyRun: Contains scripts for executing specific scenarios.
dependencies: Includes the scripts and JARs of dependencies (Mining Framework, Conflict Static Analysis, DiffJ and GroovyCSV).

### Project Requirements

It is necessary to have a DiffJ CLI in the dependencies folder and that the diff (textual diff tool) is installed.

> If you are using Windows, you will need to install [DiffUtils](http://gnuwin32.sourceforge.net/packages/diffutils.htm) manually. After installation, add the installation directory to PATH in your environment variables.

You also need to install Python version 3.7.x or later. This is required to execute scripts that obtain compilation files and convert the collected data into a format used by the static analyses of SOOT invoked by this instance.

> If you are using Windows and encounter an error during execution indicating that Python3 was not found by the system, locate the Python installation folder and create a copy of the "python" file named "python3". This will resolve the Python3 location issue on the system.

### Running the Project

The CLI has the following help page:
```
usage: java Main
 -hc <head>                 the head commit
 -pc <parents>              the parents commits
 -bc <base>                 the base commit
 -dp <ssmDependenciesPath>  path to ssm dependencies folder
 -tpr <targetProjectRoot>   path to target project root folder
 -cn <className>            packagename to main class. Eg: org.example.Main
 -m <mainMethod>            name of the main method. Eg: main
 -gp <gradlePath>           path to gradle bin
 -mp <mavenPath>            path to maven bin
 -sp <scriptsPath>          path to ssm scripts folder
 -ep <entrypoints>          entrypoints to be analyzed
```

The result will be written to `data/soot-results.csv`
