import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import { provideHttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(),
  
    { provide: DatePipe, useClass: DatePipe }   // ✅ fournir correctement
  ]
}).catch(err => console.error(err));

