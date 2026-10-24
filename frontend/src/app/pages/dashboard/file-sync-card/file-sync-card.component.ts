import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { FileStatusDTO } from '../../../core/services/api.service';

@Component({
  selector: 'app-file-sync-card',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './file-sync-card.component.html',
  styleUrl: './file-sync-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FileSyncCardComponent {
  /** Current accessibility status of the file-template base directory. */
  readonly status = input.required<FileStatusDTO>();
}
