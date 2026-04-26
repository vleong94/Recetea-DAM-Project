package com.recetea.core.recipe.application.usecases.report;

import com.recetea.core.recipe.application.ports.in.report.IGenerateGlobalInventoryReportUseCase;
import com.recetea.core.recipe.application.ports.out.report.IStatsReportPort;
import com.recetea.core.social.application.ports.in.IGetUserFavoritesUseCase;

import java.io.OutputStream;

public class GenerateGlobalInventoryReportUseCase implements IGenerateGlobalInventoryReportUseCase {

    private final IGetUserFavoritesUseCase getUserFavorites;
    private final IStatsReportPort statsReportPort;

    public GenerateGlobalInventoryReportUseCase(IGetUserFavoritesUseCase getUserFavorites,
                                                IStatsReportPort statsReportPort) {
        this.getUserFavorites = getUserFavorites;
        this.statsReportPort  = statsReportPort;
    }

    @Override
    public void execute(OutputStream outputStream) {
        statsReportPort.generateGlobalInventoryReport(getUserFavorites.execute(), outputStream);
    }
}
