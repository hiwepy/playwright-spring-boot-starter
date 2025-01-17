package com.microsoft.playwright.spring.boot.strategy;

import com.microsoft.playwright.spring.boot.enums.RenderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PlaywrightRenderStrategyRouter {

    private Map<RenderType, PlaywrightRenderStrategy> strategyMap = new HashMap<>();

    @Autowired
    public void setStrategyMap(List<PlaywrightRenderStrategy> playwrightRenderStrategyList) {
        Assert.notEmpty(playwrightRenderStrategyList, "PlaywrightRenderStrategy list can not be empty");
        this.strategyMap = playwrightRenderStrategyList.stream().collect(Collectors.toMap(PlaywrightRenderStrategy::getRenderType, strategy -> strategy));
    }


    public PlaywrightRenderStrategy route(RenderType type){
        return strategyMap.get(type);
    }

}
