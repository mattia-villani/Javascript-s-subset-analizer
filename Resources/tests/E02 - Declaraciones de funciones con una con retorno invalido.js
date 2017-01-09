function doNothing(){}
doNothing();

function consumer(int a){ 
    return ;
}
consumer(6);

function chars supplier(){
    return "Hello world";
}
supplier();

function bool test(int a){
    return a==5;
}
test(6);

function int sum(int a,int b, int c){
    return a+b+c;
}
sum(5,6,7);

function int eqAndTest( int a, int b ) {
    return a==b && test(a) && test(b);
}
eqAndTest(5,6);