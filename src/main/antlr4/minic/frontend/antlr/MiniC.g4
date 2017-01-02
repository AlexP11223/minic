grammar MiniC;

program
    : statement* EOF
    ;

statement
    : block
    | SEMI
    | assignment
    | declaration
    | 'if' parExpression statement ('else' statement)?
    | 'while' parExpression statement
    | 'break' SEMI
    | 'exit' SEMI
    | 'print' parExpression SEMI
    | 'println' parExpression SEMI
    ;

block
    : '{' statement* '}'
    ;

expression
    : literal
    | IDENTIFIER
    | ('!' | '-') expression
    | expression ('*' | '/' | '%') expression
    | expression ('+' | '-') expression
    | expression ('==' | '!=') expression
    | expression ('<' | '>' | '<=' || '>=') expression
    | expression ('&&') expression
    | expression ('||') expression
    | parExpression
    | 'readInt()'
    | 'readDouble()'
    | 'readLine()'
    | 'toString' parExpression
    ;

parExpression : '(' expression ')';

assignment : IDENTIFIER assignmentOp expression SEMI;

declaration : type IDENTIFIER (assignmentOp expression)? SEMI;

assignmentOp : '=';

type : 'int'
     | 'double'
     | 'bool'
     | 'string'
     ;

literal : IntegerLiteral
        | FloatingPointLiteral
        | StringLiteral
        | BooleanLiteral
        ;

// lexer rules (starting with uppercase)

IntegerLiteral : DIGIT+;
FloatingPointLiteral : DIGIT+ '.' DIGIT+;
StringLiteral : '"' (ESC | ~["\\])* '"' ;
BooleanLiteral : 'true' | 'false';

IDENTIFIER : (LETTER | '_') (LETTER | DIGIT | '_')* ;

SEMI : ';';

fragment
DIGIT : '0'..'9';

fragment
LETTER : ('a'..'z' | 'A'..'Z');

fragment ESC :   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;


WS  :  [ \t\r\n\u000C]+ -> skip
    ;

// tokens, needed to be able to be able to reference in visitor them via constants

MUL : '*';
DIV : '/';
PLUS : '+';
MINUS : '-';
MOD : '%';
LT : '<';
GT : '>';
LTEQ : '<=';
GTEQ : '>=';
ASSIGN : '=';
EQ : '==';
NOTEQ : '!=';
NOT : '!';
AND : '&&';
OR : '||';
