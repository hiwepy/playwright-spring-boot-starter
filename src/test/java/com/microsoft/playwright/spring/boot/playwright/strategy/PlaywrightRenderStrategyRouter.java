package com.microsoft.playwright.spring.boot.playwright.strategy;

import com.microsoft.playwright.spring.boot.playwright.enums.RenderType;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlaywrightRenderStrategyRouter {

    private Map<RenderType, PlaywrightRenderStrategy> strategyMap = new HashMap<>();

    public PlaywrightRenderStrategyRouter(List<PlaywrightRenderStrategy> playwrightRenderStrategyList) {
        Assert.notEmpty(playwrightRenderStrategyList, "PlaywrightRenderStrategy list can not be empty");
        this.strategyMap = playwrightRenderStrategyList.stream()
                .collect(Collectors.toMap(PlaywrightRenderStrategy::getRenderType, strategy -> strategy));
    }

    public PlaywrightRenderStrategy route(RenderType type){
        return strategyMap.get(type);
    }

}
