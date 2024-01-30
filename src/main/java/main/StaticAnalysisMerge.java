package main;

import DependenciesManager.DependenciesManager;
import buildManager.BuildGenerator;
import csvManager.CsvManager;
import entrypointManager.EntrypointManager;
import gitManager.CollectedMergeMethodData;
import gitManager.CommitManager;
import gitManager.MergeManager;
import gitManager.ModifiedLinesManager;
import org.apache.commons.io.FileUtils;
import project.MergeCommit;
import project.Project;
import services.outputProcessors.GenerateSootInputFilesOutputProcessor;
import services.outputProcessors.soot.RunSootAnalysisOutputProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class StaticAnalysisMerge {

    String[] args;

    StaticAnalysisMerge(String[] args){
        this.args = args;
    }

    public void run() {
        //DependenciesManager dependenciesManager = new DependenciesManager();
        MergeManager mergeManager = new MergeManager();
        BuildGenerator buildGenerator = new BuildGenerator(this.args[5], this.args[6], this.args[7]);
        CommitManager commitManager = new CommitManager(this.args);
        Project project = new Project("project", this.args[7]);
        ModifiedLinesManager modifiedLinesManager = new ModifiedLinesManager();
        EntrypointManager entrypointManager = new EntrypointManager();

        try {
            //dependenciesManager.copyAuxFilesToProject(this.args[4]);

            MergeCommit mergeCommit = commitManager.buildMergeCommit();

            Process buildGeneration = buildGenerator.generateBuild();
            buildGeneration.waitFor();

            if(buildGeneration.exitValue() != 0) {
                System.out.println("Could not generate a valid build");
                mergeManager.revertCommint(mergeCommit.getLeftSHA());
                return;
            }

            File buildJar = buildGenerator.getBuildJar();

            File dest = new File("files/project/" + mergeCommit.getSHA() + "/original-without-dependencies/merge/build.jar");
            FileUtils.copyFile(buildJar, dest);
            entrypointManager.configureSoot(dest.getPath());


            List<CollectedMergeMethodData> collectedMergeMethodDataList = modifiedLinesManager.collectData(project, mergeCommit);
            CsvManager csvManager = new CsvManager();
            csvManager.transformCollectedDataIntoCsv(collectedMergeMethodDataList, ".");
            csvManager.trimSpacesAndSpecialChars(new File("data/results-with-build-information.csv"));


            GenerateSootInputFilesOutputProcessor generateSootInputFilesOutputProcessor = new GenerateSootInputFilesOutputProcessor();
            generateSootInputFilesOutputProcessor.convertToSootScript(".");

            for(CollectedMergeMethodData data : collectedMergeMethodDataList) {
                String path = "files/"+ data.getProject().getName() + "/" + mergeCommit.getSHA() + "/changed-methods/" + data.getClassName() +"/" + data.getMethodSignature();
                path = path.replaceAll(" ", "");
                path = path.replaceAll("[+^?<>|]*", "");
                File left = new File( path + "/left-right-lines.csv");
                File right = new File(path + "/right-left-lines.csv");

                csvManager.trimBlankLines(left);
                csvManager.trimBlankLines(right);
            }


            RunSootAnalysisOutputProcessor runSootAnalysisOutputProcessor = new RunSootAnalysisOutputProcessor();
            runSootAnalysisOutputProcessor.executeAllAnalyses(".");

            File results = new File("./data/soot-results.csv");

            if(csvManager.hasConflict(results)){
                mergeManager.revertCommint(mergeCommit.getLeftSHA());
            }

            //dependenciesManager.deleteAuxFiles(this.args[4]);

        } catch (IOException | InterruptedException /*| InterruptedException e*/e) {
            e.printStackTrace();
        }
    }
}