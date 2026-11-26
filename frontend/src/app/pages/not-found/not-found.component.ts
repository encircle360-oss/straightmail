import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  templateUrl: './not-found.component.html',
  imports: [RouterLink],
})
/** Displayed when no route matches the requested URL (HTTP 404 equivalent). */
export class NotFoundComponent {}
