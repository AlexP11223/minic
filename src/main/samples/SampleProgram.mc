println("Hello world!");

print("Enter name: ");
string name = readLine();

print("Enter age: ");
int age = readInt();

if (age < 10) {
    println("Sorry, you are not old enough to learn about compilers");
    exit;
}

println("Hello " + name);

int n = 10;
int sum = 0;
int i = 1;
while (i <= n) {
    sum = sum + i;
    i = i + 1;
}
println("Sum of the first " + toString(n) +
        " natural numbers: " + toString(sum));

println("First 10 prime numbers:");
int num = 2;
int count = 0;
while (count < 10) {
    bool isPrime = true;
    int j = 1;
    while (j < num / 2) {
        if (j != 1 && num % j == 0) {
            isPrime = false;
            break;
        } else
            j = j + 1;
    }
    if (isPrime) {
        count = count + 1;
        print(toString(num));
    }
    num = num + 1;
}
