package com.github.tpgamesnl.javausagechecker.query;

public class ClassQuery implements Query {

    private final StringCheck nameCheck;

    public ClassQuery(StringCheck nameCheck) {
        this.nameCheck = nameCheck;
    }

    @Override
    public boolean checkMethodAccess(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        return false;
    }

    @Override
    public boolean checkFieldAccess(int opcode, String owner, String name, String descriptor) {
        return false;
    }

    @Override
    public boolean checkClassUsage(String name) {
        return nameCheck.match(name);
    }

    @Override
    public String toString() {
        return "ClassQuery{" +
                "nameCheck=" + nameCheck +
                '}';
    }

}
