package com.github.tpgamesnl.javausagechecker;

import com.github.tpgamesnl.javausagechecker.query.Query;

public class Report {

    private final ClassLocation classLocation;
    private final Query query;

    public Report(ClassLocation classLocation, Query query) {
        this.classLocation = classLocation;
        this.query = query;
    }

    public ClassLocation getClassLocation() {
        return classLocation;
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "usage at " + classLocation + " from query " + query;
    }

}
