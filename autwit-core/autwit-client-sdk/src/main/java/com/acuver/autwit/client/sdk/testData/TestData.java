package com.acuver.autwit.client.sdk.testData;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.XMLFormatter;

@Data
public class TestData {
    private String id;

    // business fields (change anytime)
    private Map<String,Object> fields = new HashMap<>();

    // autwit-managed fields
    private boolean active = true;
    private boolean paused = false;
    private Date createdAt = new Date();

}
// -> POST->
//
//Client, TestData, DataObject[DTO], Feature,StepDef, runner
//
//
//--> Collect Data [Excel] -> Autwit Read -> Mannul/Automaion - Generate SQL statment-> DB ready! Happy Path
//        Alter Table -> SQL -> Commite;



