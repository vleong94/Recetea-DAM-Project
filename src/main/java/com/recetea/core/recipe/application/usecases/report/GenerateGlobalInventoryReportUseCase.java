package com.recetea.core.recipe.application.usecases.report;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.report.IGenerateGlobalInventoryReportUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.report.IStatsReportPort;
import com.recetea.core.shared.domain.PageRequest;

import java.io.OutputStream;
import java.util.List;

public class GenerateGlobalInventoryReportUseCase implements IGenerateGlobalInventoryReportUseCase {

    private final IRecipeRepository repository;
    private final IStatsReportPort statsReportPort;

    public GenerateGlobalInventoryReportUseCase(IRecipeRepository repository, IStatsReportPort statsReportPort) {
        this.repository = repository;
        this.statsReportPort = statsReportPort;
    }

    @Override
    public void execute(OutputStream outputStream) {
        List<RecipeSummaryResponse> summaries = repository
                .findAllSummaries(new PageRequest(0, Integer.MAX_VALUE))
                .content();
        statsReportPort.generateGlobalInventoryReport(summaries, outputStream);
    }
}
