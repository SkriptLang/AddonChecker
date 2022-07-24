package com.github.tpgamesnl.javausagechecker.query;

public class MethodQuery implements Query {

    private final StringCheck owner;
    private final StringCheck name;
    private final StringCheck descriptor;

    public MethodQuery(StringCheck owner, StringCheck name, StringCheck descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public boolean checkMethodAccess(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        return this.owner.match(owner) && this.name.match(name) && this.descriptor.match(descriptor);
    }

    @Override
    public boolean checkFieldAccess(int opcode, String owner, String name, String descriptor) {
        return false;
    }

    @Override
    public String toString() {
        return "MethodQuery{" +
                "owner=" + owner +
                ", name=" + name +
                ", descriptor=" + descriptor +
                '}';
    }

}
