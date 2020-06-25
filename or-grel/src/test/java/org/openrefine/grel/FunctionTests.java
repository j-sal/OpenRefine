/*

Copyright 2013, Thomas F. Morris
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
 
package org.openrefine.grel;

import java.io.IOException;
import java.util.Properties;

import org.openrefine.grel.ControlFunctionRegistry;
import org.openrefine.grel.Function;
import org.openrefine.grel.Parser;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.Engine;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Row;


public class FunctionTests {

    static Properties bindings;
    Project project;
    Engine engine;

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        MetaParser.registerLanguageParser("grel", "General Refine Expression Language", Parser.grelParser, "value");
        /*
        project = new Project();
        project.columnModel.addColumn(0, new ColumnMetadata(0, "Column A"), false);
        bindings = new Properties();
        bindings.put("project", project);
        
        // Five rows of a's and five of 1s
        for (int i = 0; i < 10; i++) {
            Row row = new Row(1);
            row.setCell(0, new Cell(i < 5 ? "a":new Integer(1), null));
            project.rows.add(row);
        }
        */
    }


    @AfterMethod
    public void TearDown() {
        bindings = null;
    }
    
    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    private static Object invoke(String name,Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function "+name);
        }
        if (args == null) {
            return function.call(bindings,new Object[0]);
        } else {
            return function.call(bindings,args);
        }
    }
    
    @Test
    public void testInvalidParams() {        
        Assert.assertTrue(invoke("facetCount") instanceof EvalError);
        Assert.assertTrue(invoke("facetCount", "one","two","three") instanceof EvalError);
        Assert.assertTrue(invoke("facetCount", "one","bad(","Column A") instanceof EvalError);

    }
    
    @Test(groups = {"broken_by_spark"})
    public void testFacetCount() {        
        Assert.assertEquals(invoke("facetCount", "a", "value", "Column A"),Integer.valueOf(5));
        Assert.assertEquals(invoke("facetCount", new Integer(1), "value", "Column A"),Integer.valueOf(5));
        Assert.assertEquals(invoke("facetCount", new Integer(2), "value+1", "Column A"),Integer.valueOf(5));
    }
}