package com.recetea.core.recipe.application.ports.in.report;

import java.io.OutputStream;

public interface IGenerateGlobalInventoryReportUseCase {

    void execute(OutputStream outputStream);
}
