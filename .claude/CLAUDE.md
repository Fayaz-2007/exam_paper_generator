PROJECT: Smart Question Paper Generator



TECH STACK:

\- Frontend: HTML, CSS (Tailwind), JavaScript

\- Backend: Java (Spring Boot)

\- Database: Supabase (PostgreSQL)

\- Auth: Supabase Auth + Google OAuth

\- PDF Input: Apache PDFBox

\- PDF Export: Java backend

\- API: REST APIs with Fetch



CORE RULES:

\- Part A: 10 questions, 2 marks each, from 40-question bank

\- Part B: 3 main questions, each has (a) OR (b), 6 questions total from 25-question bank

\- RBT Rule: Only RBT 1 and RBT 2 allowed. Ignore questions with RBT > 2

\- Each question must have: Question text, CO number, RBT level, Part (A or B)



ROLES:

\- Teacher: uploads PDF, system parses and stores questions

\- HOD: selects subject + exam, clicks generate, gets preview + export



NEVER modify files outside /src without asking.

Always add comments to every function.

Always follow existing code style.

