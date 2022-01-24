/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.editing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.ReconStats;
import com.google.refine.model.Row;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;

/**
 * This keeps track of the new items that we have created for each internal
 * reconciliation id.
 * 
 * @author Antonin Delpeuch
 *
 */
public class NewEntityLibrary {

    private Map<Long, String> map;

    public NewEntityLibrary() {
        map = new HashMap<>();
    }

    @JsonCreator
    public NewEntityLibrary(@JsonProperty("qidMap") Map<Long, String> map) {
        this.map = map;
    }

    /**
     * Retrieves the id allocated to a given new cell
     * 
     * @param id:
     *            the fake EntityId generated by the cell
     * @return the id (or null if unallocated yet)
     */
    public String getId(long id) {
        return map.get(id);
    }

    /**
     * Stores an id associated to a new cell
     * 
     * @param id
     *            : the internal reconciliation id of the new cell
     * @param returnedId
     *            : the associated id returned by Wikibase
     */
    public void setId(long id, String returnedId) {
        map.put(id, returnedId);
    }

    /**
     * Changes the "new" reconciled cells to their allocated ids for later use.
     * 
     * @param reset:
     *            set to true to revert the operation (set cells to "new")
     */
    public void updateReconciledCells(Project project, boolean reset) {

        Set<Integer> impactedColumns = new HashSet<>();

        /*
         * Note that there is a slight violation of OpenRefine's model here: if we
         * reconcile multiple cells to the same new Entity, and then perform this
         * operation on a subset of the corresponding rows, we are going to modify cells
         * that are outside the facet (because they are reconciled to the same cell).
         * But I think this is the right thing to do.
         */

        for (Row row : project.rows) {
            for (int i = 0; i != row.cells.size(); i++) {
                Cell cell = row.cells.get(i);
                if (cell == null || cell.recon == null) {
                    continue;
                }
                Recon recon = cell.recon;
                boolean changed = false;
                if (Recon.Judgment.New.equals(recon.judgment) && !reset
                        && map.containsKey(recon.id)) {
                    recon.judgment = Recon.Judgment.Matched;
                    recon.match = new ReconCandidate(map.get(recon.id), cell.value.toString(),
                            new String[0], 100);
                    recon.addCandidate(recon.match);
                    changed = true;
                    
                } else if (Recon.Judgment.Matched.equals(recon.judgment) && reset
                        && map.containsKey(recon.id)) {
                    recon.judgment = Recon.Judgment.New;
                    if(recon.candidates != null) {
                    	recon.candidates.remove(recon.candidates.size()-1);
                    }
                    recon.match = null;
                    changed = true;
                }
                
                if (changed) {
	                impactedColumns.add(i);
	                // Compute features
	                Column column = project.columnModel.getColumnByCellIndex(i);
	                ReconConfig config = column.getReconConfig();
	                if (config instanceof StandardReconConfig) {
	                	StandardReconConfig stdConfig = (StandardReconConfig)config;
										if (cell.getValue() instanceof String) {
	                		stdConfig.computeFeatures(recon, (String) cell.getValue());
										}
	                }
                }
            }
        }
        // Update reconciliation statistics for impacted columns
        for (Integer colId : impactedColumns) {
            Column column = project.columnModel.getColumnByCellIndex(colId);
            column.setReconStats(ReconStats.create(project, colId));
        }
    }

    /**
     * Getter, only meant to be used by Jackson
     * 
     * @return the underlying map
     */
    @JsonProperty("qidMap")
    public Map<Long, String> getIdMap() {
        return map;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !NewEntityLibrary.class.isInstance(other)) {
            return false;
        }
        NewEntityLibrary otherLibrary = (NewEntityLibrary) other;
        return map.equals(otherLibrary.getIdMap());
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
