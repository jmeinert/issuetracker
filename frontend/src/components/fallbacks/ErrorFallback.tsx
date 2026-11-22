import { getApiErrorMessage } from '@/lib/api-client';
import { Alert, Button } from '@mantine/core';
import { FullPageCenter } from '../ui/FullPageCenter';

export function ErrorFallback({ error }: { error: Error }) {
  return (
    <FullPageCenter>
      <Alert color="red" title="Request failed">
        <p>{getApiErrorMessage(error)}</p>
        <Button
          color="black"
          variant="white"
          onClick={() => window.location.reload()}
        >
          Refresh
        </Button>
      </Alert>
    </FullPageCenter>
  );
}
