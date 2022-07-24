package com.github.tpgamesnl.javausagechecker.query;

public class FieldQuery implements Query {

    private final StringCheck owner;
    private final StringCheck name;
    private final StringCheck descriptor;

    public FieldQuery(StringCheck owner, StringCheck name, StringCheck descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public boolean checkMethodAccess(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        return false;
    }

    @Override
    public boolean checkFieldAccess(int opcode, String owner, String name, String descriptor) {
        return this.owner.match(owner) && this.name.match(name) && this.descriptor.match(descriptor);
    }

    @Override
    public String toString() {
        return "FieldQuery{" +
                "owner=" + owner +
                ", name=" + name +
                ", descriptor=" + descriptor +
                '}';
    }

}
