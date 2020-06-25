/*

Copyright 2010,2012. Google Inc.
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

package org.openrefine.model.recon;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrefine.expr.HasFields;
import org.openrefine.util.JsonViews;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.ImmutableList;
 
@JsonFilter("reconCandidateFilter")
public class Recon implements HasFields, Serializable {
    
    private static final long serialVersionUID = 3926584932566476537L;
    
    /**
     * Freebase schema URLs kept for compatibility with legacy reconciliation results
     */
    private static final String FREEBASE_SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private static final String FREEBASE_IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/type.object.mid";
    
    private static final String WIKIDATA_SCHEMA_SPACE = "http://www.wikidata.org/prop/direct/";
    private static final String WIKIDATA_IDENTIFIER_SPACE = "http://www.wikidata.org/entity/";

    static public enum Judgment {
        @JsonProperty("none")
        None,
        @JsonProperty("matched")
        Matched,
        @JsonProperty("new")
        New
    }
    
    @Deprecated
    static public String judgmentToString(Judgment judgment) {
        if (judgment == Judgment.Matched) {
            return "matched";
        } else if (judgment == Judgment.New) {
            return "new";
        } else {
            return "none";
        }
    }
    
    /**
     * Deprecated: use JSON deserialization to create
     * a Judgment object directly.
     * @param s
     * @return
     */
    @Deprecated
    static public Judgment stringToJudgment(String s) {
        if ("matched".equals(s)) {
            return Judgment.Matched;
        } else if ("new".equals(s)) {
            return Judgment.New;
        } else {
            return Judgment.None;
        }
    }
    
    static final public int Feature_typeMatch = 0;
    static final public int Feature_nameMatch = 1;
    static final public int Feature_nameLevenshtein = 2;
    static final public int Feature_nameWordDistance = 3;
    static final public int Feature_max = 4;

    static final protected Map<String, Integer> s_featureMap = new HashMap<String, Integer>();
    static {
        s_featureMap.put("typeMatch", Feature_typeMatch);
        s_featureMap.put("nameMatch", Feature_nameMatch);
        s_featureMap.put("nameLevenshtein", Feature_nameLevenshtein);
        s_featureMap.put("nameWordDistance", Feature_nameWordDistance);
    }
    
    @JsonIgnore
    final public long            id;
    @JsonIgnore
    final public String          service;
    @JsonIgnore
    final public String          identifierSpace;
    @JsonIgnore
    final public String          schemaSpace;
    
    @JsonIgnore
    final public Object[]        features;
    @JsonIgnore
    final public ImmutableList<ReconCandidate>  candidates;
    
    @JsonIgnore
    final public Judgment        judgment;
    @JsonIgnore
    final public String          judgmentAction;
    @JsonIgnore
    final public long            judgmentHistoryEntry;
    @JsonIgnore
    final public long            judgmentBatchSize;
    
    @JsonIgnore
    final public ReconCandidate  match;
    @JsonIgnore
    final public int             matchRank;
    
    @Deprecated
    static public Recon makeFreebaseRecon(long judgmentHistoryEntry) {
        return new Recon(
            judgmentHistoryEntry,
            FREEBASE_IDENTIFIER_SPACE,
            FREEBASE_SCHEMA_SPACE);
    }
    
    static public Recon makeWikidataRecon(long judgmentHistoryEntry) {
        return new Recon(
            judgmentHistoryEntry,
            WIKIDATA_IDENTIFIER_SPACE,
            WIKIDATA_SCHEMA_SPACE);
    }
    
    public Recon(long judgmentHistoryEntry, String identifierSpace, String schemaSpace) {
        id = System.currentTimeMillis() * 1000000 + Math.round(Math.random() * 1000000);
        service = "unknown";
        this.judgmentHistoryEntry = judgmentHistoryEntry;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        features = new Object[Feature_max];
        candidates = null;
        judgment = Judgment.None;
        judgmentAction = "unknown";
        judgmentHistoryEntry = 0;
        judgmentBatchSize = 0;
        match = null;
        matchRank = -1;
    }
    
    // TODO inline this
    public Recon dup(long judgmentHistoryEntry) {
        return withJudgmentHistoryEntry(judgmentHistoryEntry);
    }
    
    @JsonIgnore
    public ReconCandidate getBestCandidate() {
        if (candidates != null && candidates.size() > 0) {
            return candidates.get(0);
        }
        return null;
    }
    
    public Object getFeature(int feature) {
        return feature < features.length ? features[feature] : null;
    }
    
    @Override
    public Object getField(String name) {
        if ("id".equals(name)) {
            return id;
        } else if ("best".equals(name)) {
            return candidates != null && candidates.size() > 0 ? candidates.get(0) : null;
        } else if ("candidates".equals(name)) {
            return candidates;
        } else if ("judgment".equals(name) || "judgement".equals(name)) {
            return judgmentToString();
        } else if ("judgmentAction".equals(name) || "judgementAction".equals(name)) {
            return judgmentAction;
        } else if ("judgmentHistoryEntry".equals(name) || "judgementHistoryEntry".equals(name)) {
            return judgmentHistoryEntry;
        } else if ("judgmentBatchSize".equals(name) || "judgementBatchSize".equals(name)) {
            return judgmentBatchSize;
        } else if ("matched".equals(name)) {
            return judgment == Judgment.Matched;
        } else if ("new".equals(name)) {
            return judgment == Judgment.New;
        } else if ("match".equals(name)) {
            return match;
        } else if ("matchRank".equals(name)) {
            return matchRank;
        } else if ("features".equals(name)) {
            return new Features();
        } else if ("service".equals(name)) {
            return service;
        } else if ("identifierSpace".equals(name)) {
            return identifierSpace;
        } else if ("schemaSpace".equals(name)) {
            return schemaSpace;
        }
        return null;
    }
    
    @Override
    public boolean fieldAlsoHasFields(String name) {
        return "match".equals(name) || "best".equals(name);
    }
    
    @Deprecated
    protected String judgmentToString() {
        return judgmentToString(judgment);
    }
    
    public class Features implements HasFields {
        @Override
        public Object getField(String name) {
            int index = s_featureMap.containsKey(name) ? s_featureMap.get(name) : -1;
            return (index >= 0 && index < features.length) ? features[index] : null;
        }

        @Override
        public boolean fieldAlsoHasFields(String name) {
            return false;
        }
    }
    
    @JsonProperty("id")
    public long getId() {
        return id;
    }
    
    @JsonProperty("judgmentHistoryEntry")
    @JsonView(JsonViews.SaveMode.class)
    public long getJudgmentHistoryEntry() {
        return judgmentHistoryEntry;
    }
    
    @JsonProperty("service")
    public String getServiceURI() {
        return service;
    }
    
    @JsonProperty("identifierSpace")
    public String getIdentifierSpace() {
        return identifierSpace;
    }
    
    @JsonProperty("schemaSpace")
    public String getSchemaSpace() {
        return schemaSpace;
    }
    
    @JsonProperty("j")
    public Judgment getJudgment() {
        return judgment;
    }
    
    @JsonProperty("m")
    @JsonInclude(Include.NON_NULL)
    public ReconCandidate getMatch() {
        return match;
    }
    
    @JsonProperty("c")
    //@JsonView(JsonViews.SaveMode.class)
    public List<ReconCandidate> getCandidates() {
        if (candidates != null) {
            return candidates;
        }
        return Collections.emptyList();
    }
   
    
    @JsonProperty("f")
    @JsonView(JsonViews.SaveMode.class)
    public Object[] getfeatures() {
        return features;
    }
    
    @JsonProperty("judgmentAction")
    @JsonView(JsonViews.SaveMode.class)
    public String getJudgmentAction() {
        return judgmentAction;
    }
    
    @JsonProperty("judgmentBatchSize")
    @JsonView(JsonViews.SaveMode.class)
    public long getJudgmentBatchSize() {
        return judgmentBatchSize;
    }
    
    @JsonProperty("matchRank")
    @JsonView(JsonViews.SaveMode.class)
    @JsonInclude(Include.NON_NULL)
    public Integer getMatchRank() {
        if (match != null) {
            return matchRank;
        }
        return null;
    }

    static public Recon loadStreaming(String s) throws Exception {
        return ParsingUtilities.mapper.readValue(s, Recon.class);
    }
    
    @JsonCreator
    public Recon(
            @JsonProperty("id")
            long id,
            @JsonProperty("judgmentHistoryEntry")
            long judgmentHistoryEntry,
            @JsonProperty("j")
            Judgment judgment,
            @JsonProperty("m")
            ReconCandidate match,
            @JsonProperty("f")
            Object[] features,
            @JsonProperty("c")
            List<ReconCandidate> candidates,
            @JsonProperty("service")
            String service,
            @JsonProperty("identifierSpace")
            String identifierSpace,
            @JsonProperty("schemaSpace")
            String schemaSpace,
            @JsonProperty("judgmentAction")
            String judgmentAction,
            @JsonProperty("judgmentBatchSize")
            Long judgmentBatchSize,
            @JsonProperty("matchRank")
            Integer matchRank) {
        this.id = id;
        this.judgmentHistoryEntry = judgmentHistoryEntry;
        this.judgment = judgment != null ? judgment : Judgment.None;
        this.match = match;
        this.features = features != null ? features : new Object[Feature_max];
        this.candidates = candidates != null ? ImmutableList.copyOf(candidates) : ImmutableList.of();
        this.service = service != null ? service : "unknown";
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        this.judgmentAction = judgmentAction != null ? judgmentAction : "unknown";
        this.judgmentBatchSize = judgmentBatchSize != null ? judgmentBatchSize : 0;
        this.matchRank = matchRank != null ? matchRank : -1;
    }
    
    public Recon withId(long newId) {
        return new Recon(
                newId,
                judgmentHistoryEntry,
                judgment,
                match,
                features,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withJudgmentHistoryEntry(long newJudgmentHistoryEntry) {
        return new Recon(
                id,
                newJudgmentHistoryEntry,
                judgment,
                match,
                features,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withJudgment(Judgment newJudgment) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                newJudgment,
                match,
                features,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withMatch(ReconCandidate newMatch) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                newMatch,
                features,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withFeatures(Object[] newFeatures) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                match,
                newFeatures,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withCandidates(List<ReconCandidate> newCandidates) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                match,
                features,
                newCandidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    /**
     * Adds a reconciliation candidate at the end of the list of candidates
     * @param newCandidate
     * @return
     */
    public Recon withCandidate(ReconCandidate newCandidate) {
        ImmutableList<ReconCandidate> newCandidates = ImmutableList
                .<ReconCandidate>builder()
                .addAll(candidates)
                .add(newCandidate)
                .build();
        return withCandidates(newCandidates);
    }
    
    public Recon withService(String service) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                match,
                features,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withIdentifierSpace(String newIdentifierSpace) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                match,
                features,
                candidates,
                service,
                newIdentifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withSchemaSpace(String newSchemaSpace) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                match,
                features,
                candidates,
                service,
                identifierSpace,
                newSchemaSpace,
                judgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withJudgmentAction(String newJudgmentAction) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                match,
                features,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                newJudgmentAction,
                judgmentBatchSize,
                matchRank);
    }
    
    public Recon withJudgmentBatchSize(Long newJudgmentBatchSize) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                match,
                features,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                newJudgmentBatchSize,
                matchRank);
    }
    
    public Recon withMatchRank(int newMatchRank) {
        return new Recon(
                id,
                judgmentHistoryEntry,
                judgment,
                match,
                features,
                candidates,
                service,
                identifierSpace,
                schemaSpace,
                judgmentAction,
                judgmentBatchSize,
                newMatchRank);
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Recon)) {
            return false;
        }
        Recon otherRecon = (Recon) other;
        return (id == otherRecon.id &&
                judgmentHistoryEntry == otherRecon.judgmentHistoryEntry &&
                judgment.equals(otherRecon.judgment) &&
                ((match == null && otherRecon.match == null) || match.equals(otherRecon.match)) &&
                candidates.equals(otherRecon.candidates) &&
                service.equals(otherRecon.service) &&
                identifierSpace.equals(otherRecon.identifierSpace) &&
                schemaSpace.equals(otherRecon.schemaSpace) &&
                judgmentAction.equals(otherRecon.judgmentAction) &&
                judgmentBatchSize == otherRecon.judgmentBatchSize &&
                matchRank == otherRecon.matchRank);
    }
    
    @Override
    public int hashCode() {
        return (int) id;
    }}