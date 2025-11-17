package JsonModule;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class JsonModule {

    public static class JsonSerializationException extends Exception {

        public JsonSerializationException(String message) {
            super(message);
        }

        public JsonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }

        public JsonSerializationException(Throwable cause) {
            super(cause);
        }
    }

    public boolean IsSerializable(Object obj){
        if(obj == null) return false;
        JsonSerializable annotation =  obj.getClass().getAnnotation(JsonSerializable.class);
        return annotation != null;
    }

    public String Serialize(Object obj) throws JsonSerializationException {
        if(!IsSerializable(obj))
            throw new JsonSerializationException("Class is not annotated with @JsonSerializable.");

        return SerializeRecursive(obj,0, false);
    }

    private String SerializeRecursive(Object obj, int level, boolean insideBracket) throws  JsonSerializationException {
        try{
            StringBuilder builder = new StringBuilder();
            Field[] fields = obj.getClass().getDeclaredFields();
            fields = Arrays.stream(fields).filter(
                            x -> isJsonElementAndNotEmpty(x, obj))
                    .toArray(Field[]::new);

            builder.append("\t".repeat(level));
            builder.append("{");
            builder.append("\n");

            for(int i = 0; i <fields.length; i++){
                Field field = fields[i];
                String key = field.getName();
                JsonElement annotation = field.getAnnotation(JsonElement.class);
                if(annotation.key() != null && annotation.key().trim().length() > 0) {
                    // user can define custom keys
                    key = annotation.key();
                }

                builder.append("\t".repeat(level +1));
                builder.append("\""+ key + "\"");
                builder.append(" : ");

                Object value = field.get(obj);
                if(isPrimitive(value)) {
                    builder.append("\""+ String.valueOf(value) + "\"");
                }
                else if(value.getClass().isArray()) {
                    builder.append("[ ");
                    builder.append("\n");
                    for(int j = 0; j < Array.getLength(value); j++){
                        String serialized_child = SerializeListItem(Array.get(value, j));
                        builder.append(serialized_child);
                        if(j < Array.getLength(value) -1) {
                            builder.append(", ");
                        }
                        builder.append("\n");
                    }
                    builder.append("\t".repeat(level));
                    builder.append("]");
                }
                else if(value instanceof List<?> || value instanceof ArrayList<?>) {
                    builder.append("[ ");
                    builder.append("\n");
                    for(int j = 0; j < ((List<Object>)value).size(); j++){
                        String serialized_child = SerializeListItem(((List<Object>)value).get(j));
                        builder.append(serialized_child);
                        if (j < ((List<Object>)value).size() - 1) {
                            builder.append(",\n");
                        }
                    }
                    builder.append("\t".repeat(level));
                    builder.append("]");
                }
                else {
                    if(!IsSerializable(value)) {
                        throw new JsonSerializationException("Class is not annotated with @JsonSerializable.");
                    }
                    String child_json = SerializeListItem(value);
                    builder.append(child_json);
                }

                if(i < fields.length -1) builder.append(",");
                builder.append("\n");
            }
            builder.append("\t".repeat(level));
            builder.append("}");
            if (!insideBracket) {
                builder.append("\n");
            }

            return builder.toString();
        }catch (Exception ex) {
            throw new JsonSerializationException(ex.getLocalizedMessage());
        }

    }

    private String SerializeListItem(Object item) throws JsonSerializationException {
        if(isPrimitive(item)) return  "\"" + String.valueOf(item) + "\"";
        return  Serialize(item);
    }
    private boolean isJsonElementAndNotEmpty(Field field, Object classObject){
        JsonElement attribute = field.getDeclaredAnnotation(JsonElement.class);

        if(attribute == null) return false;

        try{
            field.setAccessible(true);
            Object value = field.get(classObject);
            if(value == null) return false;

            return  true;
        }catch (Exception ex) {
            return  false;
        }
    }

    private boolean isPrimitive(Object obj) {
        if (obj == null) return false;

        String className = obj.getClass().getName();

        return className.equals(Boolean.class.getName()) ||
                className.equals(Byte.class.getName()) ||
                className.equals(Short.class.getName()) ||
                className.equals(Integer.class.getName()) ||
                className.equals(Long.class.getName()) ||
                className.equals(Float.class.getName()) ||
                className.equals(Double.class.getName()) ||
                className.equals(Character.class.getName()) ||
                className.equals(String.class.getName())||

                className.equals(boolean.class.getName()) ||
                className.equals(byte.class.getName()) ||
                className.equals(short.class.getName()) ||
                className.equals(int.class.getName()) ||
                className.equals(long.class.getName()) ||
                className.equals(float.class.getName()) ||
                className.equals(double.class.getName()) ||
                className.equals(char.class.getName());
    }

    private  boolean isPrimitive(String fieldType){
        return  fieldType.equals(int.class.getName()) ||
                fieldType.equals(Integer.class.getName()) ||
                fieldType.equals(long.class.getName()) ||
                fieldType.equals(Long.class.getName()) ||
                fieldType.equals(double.class.getName()) ||
                fieldType.equals(Double.class.getName()) ||
                fieldType.equals(float.class.getName()) ||
                fieldType.equals(Float.class.getName()) ||
                fieldType.equals(boolean.class.getName()) ||
                fieldType.equals(Boolean.class.getName()) ||
                fieldType.equals(char.class.getName()) ||
                fieldType.equals(Character.class.getName()) ||
                fieldType.equals(short.class.getName()) ||
                fieldType.equals(Short.class.getName()) ||
                fieldType.equals(byte.class.getName()) ||
                fieldType.equals(Byte.class.getName()) ||
                fieldType.equals(String.class.getName());
    }

    public String minify(String json) {
        StringBuilder result = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"') {
                boolean escaped = (i > 0 && json.charAt(i - 1) == '\\');
                if (!escaped) {
                    inQuotes = !inQuotes;
                }
                result.append(c);
            }
            else if (Character.isWhitespace(c) && !inQuotes) {
                continue;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private class Lexer{
        public static enum TokenType {
            LEFT_BRACE, // {
            RIGHT_BRACE, // }
            LEFT_BRACKET, // [
            RIGHT_BRACKET, // ]
            COLON, // :
            COMMA, // ,
            STRING, // [a-zA-Z0-9]*
        }

        public static class Token {
            public TokenType type;
            public String lexeme;

            public Token(TokenType type, String lexeme) {
                this.type = type;
                this.lexeme = lexeme;
            }
            public Token(){}
            @Override
            public String toString() {
                return String.format("Token{type=%s, lexeme='%s'}", type, lexeme);
            }
        }

        private String json;
        private int lookAhead = 0;

        private ArrayList<Token> tokenStream;

        public String peek(){
            if(this.lookAhead >= json.length()) return null;
            return String.valueOf(this.json.charAt(this.lookAhead));
        }

        public String move(){
            String item = this.peek();
            if(item != null) this.lookAhead += 1;
            return item;
        }

        public ArrayList<Token> createTokenStream(String json) throws Exception {
            lookAhead = 0 ; // reset look ahead
            this.json = minify(json); // gurantee minify json in case client dont send minified
            tokenStream = new ArrayList<>(); // refresh token stream

            while (lookAhead < this.json.length()) {
                String lookAheadChar = this.peek();
                if(lookAheadChar.equals("\"")) {
                    Token token = getStringToken();
                    tokenStream.add(token);
                }
                else if (
                        lookAheadChar.equals("{") ||
                                lookAheadChar.equals("}") ||
                                lookAheadChar.equals("[") ||
                                lookAheadChar.equals("]") ||
                                lookAheadChar.equals(":") ||
                                lookAheadChar.equals(",")
                ) {
                    Token token = new Token();
                    token.lexeme = lookAheadChar;
                    token.type = classifyToken(lookAheadChar);
                    tokenStream.add(token);
                    this.move(); // consume char
                }
            }

            return  tokenStream;
        }

        private Token getStringToken() throws Exception{
            String lookAheadChar = this.peek();
            if(!lookAheadChar.equals("\"")) throw new Exception("String must start with \" ");
            this.move(); // consume "
            String  lexeme = "";
            lookAheadChar = this.peek();
            while (!lookAheadChar.equals("\"")) {
                lexeme += this.peek();
                this.move(); // consume char
                lookAheadChar = this.peek();
            }
            lookAheadChar = this.peek();
            if(!lookAheadChar.equals("\"")) throw new Exception("String must end with \" ");
            this.move(); // consume "

            Token token = new Token();
            token.lexeme =lexeme;
            token.type= TokenType.STRING;

            return  token;
        }

        private TokenType classifyToken(String lexeme) {
            switch (lexeme) {
                case "{": return TokenType.LEFT_BRACE;
                case "}": return TokenType.RIGHT_BRACE;
                case "[": return TokenType.LEFT_BRACKET;
                case "]": return TokenType.RIGHT_BRACKET;
                case ":": return TokenType.COLON;
                case ",": return TokenType.COMMA;
                default:
                    return null;
            }
        }



    }

    private class Parser {

        /**
         JSON           → Object

         Object         → '{' Members '}' | '{' '}'

         Members        → Pair MoreMembers

         MoreMembers    → ',' Pair MoreMembers | ε

         Pair           → String ':' Value

         Array          → '[' Elements ']'| '[' ']'

         Elements       → Value MoreElements

         MoreElements   → ',' Value MoreElements | ε

         Value          → String
         | Object
         | Array
         */

        private enum ParseNodeType {
            JSON,  // The entry point of the grammar (represents an entire JSON document)

            OBJECT,         // Represents a JSON object {...}
            MEMBERS,
            // Represents the list of key-value pairs inside an object
            MORE_MEMBERS,   // Represents additional pairs (comma-separated)
            PAIR,           // Represents a single "key": value pair
            ARRAY,          // Represents a JSON array [...]
            ELEMENTS,       // Represents the list of values inside an array
            MORE_ELEMENTS,  // Represents additional elements (comma-separated)
            VALUE,          // Represents a JSON value (string, number, object, array, true, false, null)

            LEFT_BRACE,     // {
            RIGHT_BRACE,    // }
            LEFT_BRACKET,   // [
            RIGHT_BRACKET,  // ]
            COLON,          // :
            COMMA,          // ,

            STRING,         // [a-zA-Z][a-zA-Z0-9]*

            EPS, // empty string

        }
        private static class ParseNode {
            private ParseNodeType type;
            private Lexer.Token token;
            private List<ParseNode> children = new ArrayList<>();

            public ParseNode(ParseNodeType type) {
                this(type, null);
            }

            public ParseNode(ParseNodeType type, Lexer.Token token) {
                this.type = type;
                this.token = token;
                this.children = new ArrayList<>();
            }

            public ParseNode(){}
            public void addChild(ParseNode child) {
                children.add(child);
            }

            public ParseNodeType getType() {
                return type;
            }

            public Lexer.Token getToken() {
                return token;
            }

            public List<ParseNode> getChildren() {
                return children;
            }

            public void printTree(String indent) {
                System.out.print(indent + "- " + type);
                if (token != null && token.lexeme != null) {
                    System.out.print(" (" + token.lexeme + ")");
                }
                System.out.println();
                for (ParseNode child : children) {
                    child.printTree(indent + "  ");
                }
            }

        }
        private ArrayList<Lexer.Token> tokenStream;
        private int lookAhead = 0;
        private Lexer.Token peek(){
            if(lookAhead >= tokenStream.size()) return  null;
            return tokenStream.get(lookAhead);
        }
        private Lexer.Token move(){
            Lexer.Token token= this.peek();
            if(token != null) lookAhead += 1;
            return token;
        }
        public ParseNode Parse(ArrayList<Lexer.Token> tokenStream) throws Exception {
            this.tokenStream = tokenStream; // refresh token stream
            return this.ParseObject();

        }

        public ParseNode ParseObject() throws Exception{
            // json does not contain tail recursion dont use loops to eleminate tail recursion

            ParseNode root = new ParseNode();
            root.type = ParseNodeType.OBJECT;

            Lexer.Token lookAheadToken = this.peek();
            ParseNode leftBraceNode = new ParseNode();
            leftBraceNode.type = ParseNodeType.LEFT_BRACE;
            leftBraceNode.token = lookAheadToken;

            root.children.add(leftBraceNode);

            this.move(); // consume {

            lookAheadToken = this.peek();

            if(!lookAheadToken.lexeme.equals("}")) {
                ParseNode parseMembersNode = this.ParseMembers();
                root.children.add(parseMembersNode);
            }

            lookAheadToken = this.peek();
            ParseNode rightBraceNode = new ParseNode();
            rightBraceNode.type = ParseNodeType.RIGHT_BRACE;
            rightBraceNode.token = lookAheadToken;

            root.children.add(rightBraceNode);
            this.move(); // consume }

            return  root;
        }

        public ParseNode ParseMembers() throws Exception{
            ParseNode root = new ParseNode();
            root.type = ParseNodeType.MEMBERS;

            ParseNode pairNode = this.ParsePair();
            ParseNode moreMembersNode = this.ParseMoreMembers();
            root.children.add(pairNode);
            root.children.add(moreMembersNode);

            return  root;
        }

        public ParseNode ParseMoreMembers() throws Exception {
            ParseNode root = new ParseNode();
            root.type = ParseNodeType.MORE_MEMBERS;

            Lexer.Token firstLookToken = this.peek();
            if(!firstLookToken.lexeme.equals(",")) {
                root.children.add(new ParseNode(ParseNodeType.EPS));
                return root;
            }

            ParseNode commaNode = new ParseNode();
            commaNode.type = ParseNodeType.COMMA;
            commaNode.token = firstLookToken;
            root.children.add(commaNode);

            this.move(); // consume ,

            ParseNode pairNode = this.ParsePair();
            ParseNode moreMembersNode = this.ParseMoreMembers();
            root.children.add(pairNode);
            root.children.add(moreMembersNode);

            return  root;
        }

        public ParseNode ParsePair() throws  Exception {
            ParseNode root = new ParseNode();
            root.type = ParseNodeType.PAIR;
            ParseNode stringNode = this.ParseString();
            root.children.add(stringNode);

            Lexer.Token token = this.peek();
            if(!token.lexeme.equals(":"))
                throw new Exception(": required to split key and value");

            ParseNode colonNode = new ParseNode();
            colonNode.type= ParseNodeType.COLON;
            root.children.add(colonNode);

            this.move(); // consume :

            ParseNode valueNode = this.ParseValue();
            root.children.add(valueNode);

            return  root;
        }

        public ParseNode ParseValue() throws Exception {
            ParseNode root = new ParseNode();
            root.type = ParseNodeType.VALUE;

            Lexer.Token token = this.peek();
            if(token.lexeme.equals("[")) {
                ParseNode child = ParseArray();
                root.children.add(child);
            }
            else if(token.lexeme.equals("{")) {
                ParseNode child = ParseObject();
                root.children.add(child);
            }
            else{
                // default pares string
                ParseNode child = ParseString();
                root.children.add(child);
            }

            return  root;
        }

        public ParseNode ParseString() throws Exception {
            ParseNode root = new ParseNode();
            root.type = ParseNodeType.STRING;

            Lexer.Token token = this.peek();
            if (token == null || token.type != Lexer.TokenType.STRING) {
                throw new RuntimeException("Expected STRING token, found: "
                        + (token == null ? "null" : token.type));
            }

            root.token = token;
            this.move(); // consume the string token

            return root;
        }

        public ParseNode ParseArray() throws Exception{
            ParseNode root = new ParseNode();
            root.type = ParseNodeType.ARRAY;

            Lexer.Token token = this.peek();
            if(!token.lexeme.equals("["))
                throw new Exception("array should start with [ char");

            ParseNode lBracketNode = new ParseNode();
            lBracketNode.type = ParseNodeType.LEFT_BRACKET;
            lBracketNode.token = token;
            root.children.add(lBracketNode);

            this.move(); // consume [

            token = this.peek();
            if(!token.lexeme.equals("]")) {
                ParseNode parseElemNode = ParseElements();
                root.children.add(parseElemNode);

                token = this.peek();
            }

            ParseNode rBracketNode  = new ParseNode();
            rBracketNode.type = ParseNodeType.RIGHT_BRACKET;
            rBracketNode.token = token;
            root.children.add(rBracketNode);

            this.move(); // consume ]

            return  root;
        }

        public ParseNode ParseElements() throws  Exception{
            ParseNode root = new ParseNode();
            root.type = ParseNodeType.ELEMENTS;

            ParseNode valueNode = ParseValue();
            root.children.add(valueNode);

            ParseNode moreElementsNode = ParseMoreElements();
            root.children.add(moreElementsNode);

            return root;
        }

        public ParseNode ParseMoreElements() throws Exception{
            ParseNode root = new ParseNode();
            root.type = ParseNodeType.MORE_ELEMENTS;
            Lexer.Token token = this.peek();
            if(!token.lexeme.equals(",")) {
                ParseNode epsNode = new ParseNode();
                epsNode.type = ParseNodeType.EPS;
                root.children.add(epsNode);
                return root;
            }

            ParseNode commaNode = new ParseNode();
            commaNode.type = ParseNodeType.COMMA;
            commaNode.token = token;
            root.children.add(commaNode);
            this.move(); // consume ,

            ParseNode valueNode = ParseValue();
            root.children.add(valueNode);

            ParseNode moreElemNode = ParseMoreElements();
            root.children.add(moreElemNode);

            return root;
        }

    }

    private HashMap<String, Object> parseRootToHashMap(Parser.ParseNode root){
        HashMap<String, Object> map = new HashMap<>();

        if(root.type.equals(Parser.ParseNodeType.MEMBERS)) {
            ArrayList<Parser.ParseNode> pairNodes = CollectPairs(root);
            for(Parser.ParseNode pair : pairNodes){
                Parser.ParseNode stringNode = pair.children.get(0);
                Parser.ParseNode valueNode = pair.children.get(2);
                Parser.ParseNode dataNode = valueNode.children.get(0);

                if(dataNode.type.equals(Parser.ParseNodeType.OBJECT)) {
                    map.put(stringNode.token.lexeme, parseRootToHashMap(dataNode));
                }
                else if(dataNode.type.equals(Parser.ParseNodeType.ARRAY)) {
                    ArrayList<Parser.ParseNode> arrayData = CollectArrayValues(dataNode);
                    ArrayList<Object> datum = new ArrayList<>();

                    for(Parser.ParseNode node : arrayData){
                        if(node.type.equals(Parser.ParseNodeType.STRING)) {
                            datum.add(node.token.lexeme);
                        }
                        if(node.type.equals(Parser.ParseNodeType.OBJECT)) {
                            datum.add(parseRootToHashMap(node));
                        }
                    }

                    map.put(stringNode.token.lexeme, datum);
                }
                else {
                    // STRING
                    map.put(stringNode.token.lexeme, dataNode.token.lexeme);
                }
            }
        }
        if(root.type.equals(Parser.ParseNodeType.MEMBERS)) {
            return map;
        }

        for(Parser.ParseNode child :  root.children){
            HashMap<String, Object> datum = parseRootToHashMap(child);
            if(datum !=  null)  {
                map.putAll(datum);
            }

        }
        return map;
    }

    public ArrayList<Parser.ParseNode> CollectPairs(Parser.ParseNode root){
        Stack<Parser.ParseNode> stack = new Stack<>();
        stack.add(root);
        ArrayList<Parser.ParseNode> pairNodes = new ArrayList<>();
        while (!stack.isEmpty()) {
            Parser.ParseNode curNode = stack.pop();
            if(curNode.type.equals(Parser.ParseNodeType.PAIR)) {
                pairNodes.add(curNode);
            }
            else {
                stack.addAll(curNode.children);
            }
        }return  pairNodes;
    }

    public ArrayList<Parser.ParseNode> CollectArrayValues(Parser.ParseNode root) {
        Stack<Parser.ParseNode> stack = new Stack<>();
        ArrayList<Parser.ParseNode> values = new ArrayList<>();
        stack.add(root);
        while (!stack.isEmpty()) {
            Parser.ParseNode curNode = stack.pop();
            if(curNode.type.equals(Parser.ParseNodeType.STRING) ||
                    curNode.type.equals(Parser.ParseNodeType.OBJECT)) {
                values.add(curNode);
            }
            else {
                stack.addAll(curNode.children);
            }
        }

        Collections.reverse(values);
        return values;
    }

    private <T> T mapToObject(HashMap<String, Object> map, Class<?> clazz) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Set<String> keys = map.keySet();
        Field[] fields = clazz.getDeclaredFields();
        fields = Arrays.stream(fields).filter(
                        f -> f.isAnnotationPresent(JsonElement.class))
                .toArray(Field[]::new);

        for(int i = 0; i < fields.length; i++){
            Field field = fields[i];
            String nameOf = field.getName();
            Class<?> type = field.getType();
            Object value = map.get(nameOf);
            if(value == null) continue;
            var fieldType = type.getName();

            // primitive types
            if (isPrimitive(fieldType))
            {
                    Object converted = convertValue(value, fieldType);
                    field.setAccessible(true);
                    field.set(instance, converted);
            }
            else if(fieldType.equals(ArrayList.class.getName())
                    || fieldType.equals(List.class.getName()) ) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;

                    String elementType = parameterizedType
                            .getActualTypeArguments()[0].getTypeName();

                    if(isPrimitive(elementType)) {
                        ArrayList<Object> data = new ArrayList<>();
                        for(Object datum : (ArrayList<Object>)value){
                            data.add(convertValue(datum, elementType));
                        }

                        field.setAccessible(true);
                        field.set(instance, data);
                    }
                    else {
                        ArrayList<Object> data = new ArrayList<>();
                        Class<?> elementClass = Class.forName(elementType);

                        for (Object datum : (ArrayList<Object>) value) {
                            if (datum instanceof Map) {
                                data.add(mapToObject((HashMap<String, Object>) datum, elementClass));
                            } else {
                                data.add(datum);
                            }
                        }

                        field.setAccessible(true);
                        field.set(instance, data);
                    }
                }
            }

            else if (type.isArray()) {
                Class<?> elementType = type.getComponentType();
                Object array = null;

                if (value instanceof List<?>) {
                    List<?> listValue = (List<?>) value;
                    int length = listValue.size();
                    array = java.lang.reflect.Array.newInstance(elementType, length);

                    for (int j = 0; j < length; j++) {
                        Object element = listValue.get(j);
                        Object converted;

                        if (isPrimitive(elementType.getName())) {
                            converted = convertValue(element, elementType.getName());
                        } else if (element instanceof Map) {
                            converted = mapToObject((HashMap<String, Object>) element, elementType);
                        } else {
                            converted = element;
                        }

                        java.lang.reflect.Array.set(array, j, converted);
                    }
                }
                else if (value.getClass().isArray()) {
                    int length = java.lang.reflect.Array.getLength(value);
                    array = java.lang.reflect.Array.newInstance(elementType, length);
                    for (int j = 0; j < length; j++) {
                        Object element = java.lang.reflect.Array.get(value, j);
                        java.lang.reflect.Array.set(array, j, element);
                    }
                } else {
                    throw new IllegalArgumentException("Expected List or array, got: " + value.getClass());
                }

                field.setAccessible(true);
                field.set(instance, array);
            }



            else {
                Type genericType = field.getGenericType();

                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    String elementType = parameterizedType.getActualTypeArguments()[0].getTypeName();
                    Class<?> elementClass = Class.forName(elementType);

                    Object data = mapToObject((HashMap<String, Object>) value, elementClass);
                    field.setAccessible(true);
                    field.set(instance, data);
                } else {
                    Class<?> elementClass = Class.forName(fieldType);
                    Object data = mapToObject((HashMap<String, Object>) value, elementClass);
                    field.setAccessible(true);
                    field.set(instance, data);
                }
            }
        }

        return  (T)instance;
    }

    private Object convertValue(Object value, String targetType) {
        if (value == null) return null;

        if (targetType.equals(int.class.getName()))
            return Integer.parseInt(value.toString());

        if (targetType.equals(Integer.class.getName()))
            return Integer.valueOf(value.toString());

        if (targetType.equals(long.class.getName()))
            return Long.parseLong(value.toString());

        if (targetType.equals(Long.class.getName()))
            return Long.valueOf(value.toString());

        if (targetType.equals(double.class.getName()))
            return Double.parseDouble(value.toString().replace(',', '.'));

        if (targetType.equals(Double.class.getName()))
            return Double.valueOf(value.toString().replace(',', '.'));

        if (targetType.equals(float.class.getName()))
            return Float.parseFloat(value.toString().replace(',', '.'));

        if (targetType.equals(Float.class.getName()))
            return Float.valueOf(value.toString().replace(',', '.'));

        if (targetType.equals(boolean.class.getName()))
            return Boolean.parseBoolean(value.toString());

        if (targetType.equals(Boolean.class.getName()))
            return Boolean.valueOf(value.toString());

        if (targetType.equals(short.class.getName()))
            return Short.parseShort(value.toString());

        if (targetType.equals(Short.class.getName()))
            return Short.valueOf(value.toString());

        if (targetType.equals(byte.class.getName()))
            return Byte.parseByte(value.toString());

        if (targetType.equals(Byte.class.getName()))
            return Byte.valueOf(value.toString());

        if (targetType.equals(char.class.getName()))
            return value.toString().isEmpty() ? '\0' : value.toString().charAt(0);

        if (targetType.equals(Character.class.getName()))
            return value.toString().isEmpty() ? Character.valueOf('\0') : Character.valueOf(value.toString().charAt(0));

        if (targetType.equals(String.class.getName()))
            return value.toString();

        return value;
    }


    public <T> T Deserialize(String json, Class<T> clazz) throws Exception {
        Lexer lexer = new Lexer();
        json = this.minify(json);
        ArrayList<Lexer.Token> tokenStream = lexer.createTokenStream(json);
        Parser parser = new Parser();
        Parser.ParseNode root = parser.Parse(tokenStream);
        HashMap<String, Object> json_map = parseRootToHashMap(root);

        // hash map to class instance
        return  mapToObject(json_map, clazz);
    }


}
