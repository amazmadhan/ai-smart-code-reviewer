
# AI-Powered Code Reviewer, Refactor & Scoring Engine — Final Bundle

This project demonstrates the full requested flow:
1. Upload Java file in the UI.
2. Backend analyzes using JavaParser heuristics + OpenAI (AI suggestions) and returns an original score out of 100 and a list of issues (with line numbers).
3. User can trigger Auto-refactor which asks the backend to perform safe AST refactors and call OpenAI to generate an improved file; the backend returns the refactored source, new AI score, and issues (if any) after refactor.
4. Both original and refactored sources are downloadable from the UI.

## How to configure OpenAI key
Place your key in `backend/src/main/resources/application.properties` as:
```
openai.api.key=Open AI keys should be placed
```
or export the env var `OPENAI_API_KEY`.

## Build backend
cd backend

mvn clean install

mvn clean package
# produces target/codereview.war
# Run standalone
java -jar target/codereview.war

## Run frontend
cd frontend

npm install

npm run dev

## Test
- Use the UI at the dev server and upload a `.java` file.
- Or use Postman collection `postman/CodeReviewer.postman_collection.json` to call /api/analyze and /api/refactor.

Postman API hit:
http method - POST

url - http://localhost:8080/api/analyze

Body -> form-data:
Key -> file File (dropdown)
Value -> upload file

frontend / UI hit:
http://localhost:5173/

postman collection link - https://.postman.co/workspace/My-Workspace~1e814d42-41de-47ff-81e3-cdef2629abae/request/15456597-c5bf405c-d9bd-450a-969d-159a77644daf?action=share&creator=15456597&ctx=documentation
