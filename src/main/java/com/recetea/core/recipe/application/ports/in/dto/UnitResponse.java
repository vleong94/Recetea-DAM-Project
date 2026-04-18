package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.UnitId;

public record UnitResponse(
        UnitId id,
        String name
) {}
