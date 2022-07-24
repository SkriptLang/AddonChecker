package com.github.tpgamesnl.javausagechecker.query;

public interface Query {

    boolean checkMethodAccess(int opcode, String owner, String name, String descriptor, boolean isInterface);

    boolean checkFieldAccess(int opcode, String owner, String name, String descriptor);

}
