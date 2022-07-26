package com.github.tpgamesnl.javausagechecker;

public class ClassLocation {

    protected final String jarFileName;
    protected final String className;

    public ClassLocation(String jarFileName, String className) {
        this.jarFileName = jarFileName;
        this.className = className;
    }

    public String getJarFileName() {
        return jarFileName;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "File: " + jarFileName + " in class " + className;
    }

    public static class Method extends ClassLocation {
        protected final String methodName;
        protected final String methodDescriptor;

        private Method(String jarFileName, String className, String methodName, String methodDescriptor) {
            super(jarFileName, className);
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
        }

        @Override
        public String toString() {
            return super.toString() + " in method " + methodName + " (" + methodDescriptor + ")";
        }

        public static class Code extends Method {
            protected final int lineNumber;

            private Code(String jarFileName, String className, String methodName, String methodDescriptor, int lineNumber) {
                super(jarFileName, className, methodName, methodDescriptor);
                this.lineNumber = lineNumber;
            }

            @Override
            public String toString() {
                return super.toString() + " on line " + lineNumber;
            }
        }

        public Code inCode(int lineNumber) {
            return new Code(jarFileName, className, methodName, methodDescriptor, lineNumber);
        }

        public static class Parameter extends Method {
            protected final int index;

            private Parameter(String jarFileName, String className, String methodName, String methodDescriptor, int index) {
                super(jarFileName, className, methodName, methodDescriptor);
                this.index = index;
            }

            @Override
            public String toString() {
                return super.toString() + " in parameter " + index;
            }
        }

        public Parameter inParameter(int index) {
            return new Parameter(jarFileName, className, methodName, methodDescriptor, index);
        }

        public static class ReturnType extends Method {
            private ReturnType(String jarFileName, String className, String methodName, String methodDescriptor) {
                super(jarFileName, className, methodName, methodDescriptor);
            }

            @Override
            public String toString() {
                return super.toString() + " in return type";
            }
        }

        public ReturnType inReturnType() {
            return new ReturnType(jarFileName, className, methodName, methodDescriptor);
        }
    }

    public Method inMethod(String methodName, String methodDescriptor) {
        return new Method(jarFileName, className, methodName, methodDescriptor);
    }

    public static class Field extends ClassLocation {
        private final String name;

        public Field(String jarFileName, String className, String name) {
            super(jarFileName, className);
            this.name = name;
        }

        @Override
        public String toString() {
            return super.toString() + " in field + " + name;
        }
    }

    public Field inField(String fieldName) {
        return new Field(jarFileName, className, fieldName);
    }

    public static class ExtendsClass extends ClassLocation {
        public ExtendsClass(String jarFileName, String className) {
            super(jarFileName, className);
        }

        @Override
        public String toString() {
            return super.toString() + " in extending class";
        }
    }

    public ExtendsClass extendsClass() {
        return new ExtendsClass(jarFileName, className);
    }

    public static class ImplementsClass extends ClassLocation {
        public ImplementsClass(String jarFileName, String className) {
            super(jarFileName, className);
        }

        @Override
        public String toString() {
            return super.toString() + " in implementing class";
        }
    }

    public ImplementsClass implementsClass() {
        return new ImplementsClass(jarFileName, className);
    }

}
