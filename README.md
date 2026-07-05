# MyAgent — Android app

A native Android app that lets Claude control your phone: send SMS, read contacts,
control clipboard, show notifications, get location, open URLs, vibrate, and check battery.

## How it works

- The app has a text box where you type a task ("text Mom that I'll be late").
- It sends that task to the Claude API with a list of tools it can call.
- Claude decides which tool(s) to use; the app executes them directly using Android's
  own APIs (no Termux needed) and shows the result.
- Your Anthropic API key is stored only on your device (SharedPreferences).

## Build it (from your phone, no PC needed)

1. Create a free GitHub account if you don't have one.
2. Create a new **empty** repository, e.g. `myagent-app`.
3. In Termux:
   ```
   pkg install git
   cd MyAgentApp
   git init
   git remote add origin https://github.com/YOUR_USERNAME/myagent-app.git
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git push -u origin main
   ```
   (You'll need a GitHub Personal Access Token as your password when it asks —
   generate one at github.com → Settings → Developer settings → Personal access tokens.)
4. Go to your repo on GitHub → **Actions** tab. The "Build APK" workflow runs automatically.
5. When it finishes (a few minutes), open the workflow run → scroll to **Artifacts** →
   download `MyAgent-debug-apk` (a zip containing `app-debug.apk`).
6. On your phone, open that zip/apk from your Downloads or file manager.
   You'll need to allow "Install unknown apps" for your browser/file manager the first time.

## First run

1. Open the app, grant the permissions it asks for (SMS, contacts, location).
2. Paste your Anthropic API key into the top field.
3. Type a task and hit Run.

## Notes

- This is a debug build — fine for personal use, not for distribution.
- SMS/contacts require Android to trust the app; some phones (including budget ones)
  may show extra warnings for apps installed outside the Play Store — this is normal.
- If a build fails, check the Actions log — usually a Gradle/SDK version mismatch,
  tell me the error and I'll fix the project files.
