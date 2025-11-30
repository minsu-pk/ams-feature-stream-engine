package com.kbank.ams.featurestreamengine.domain.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import com.kbank.ams.featurestreamengine.domain.flow.process.Process;

@Slf4j
@Getter
@ToString
public class Flow {
    private final String apiPath;
    private final String name;
    private final List<Process> processes = new ArrayList<>();

    public Flow(String apiPath,Map<String,Object> spec) {
        this.apiPath = apiPath;
        this.name = spec.get("name").toString();
    }

    public void addProcess(Process process){
        this.processes.add(process);
    }

    public Map<String,Object> run(Map<String,Object> item){
        for (Process process : this.processes) {
            item = process.getHandler().handle(item, process.getAttribute());
            if (item == null) return null;
        }
        return item;
    }
}
